package com.recall.app.core.vector.segmented

import com.recall.app.core.vector.PersistableVectorIndex
import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.bitmap.DeletionBitmap
import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import com.recall.app.core.vector.segment.SegmentHnswSearch
import com.recall.app.core.vector.segment.SegmentInfo
import com.recall.app.core.vector.segment.SegmentManifest
import com.recall.app.core.vector.segment.SegmentReader
import com.recall.app.core.vector.segment.SegmentWriter
import com.recall.app.core.vector.segment.VectorPosting
import com.recall.app.core.vector.segment.VectorPostingStore
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SegmentedVectorIndex private constructor(
    private val dimensions: Int,
    private val segmentsDir: File,
    private val manifest: SegmentManifest,
    private val postingStore: VectorPostingStore,
    private val flushThreshold: Int,
    private val m: Int,
    private val efConstruction: Int,
    private val efSearch: Int,
    private val exactSearchThreshold: Int,
) : PersistableVectorIndex {

    private data class FrozenSegment(
        val info: SegmentInfo,
        val reader: SegmentReader,
        val deletionBitmap: DeletionBitmap,
        val localIndexToMediaId: LongArray,
    )

    private val lock = ReentrantReadWriteLock()
    private val staging = HnswIndex(dimensions, m, efConstruction, efSearch)
    private val frozenSegments = LinkedHashMap<Long, FrozenSegment>()
    private val nextSegmentId = AtomicLong(System.currentTimeMillis())

    companion object {
        suspend fun open(
            dimensions: Int,
            segmentsDir: File,
            manifest: SegmentManifest,
            postingStore: VectorPostingStore,
            flushThreshold: Int = 1000,
            m: Int = 16,
            efConstruction: Int = 200,
            efSearch: Int = 50,
            exactSearchThreshold: Int = 2000,
        ): SegmentedVectorIndex {
            require(dimensions > 0)
            require(flushThreshold > 0)
            require(exactSearchThreshold >= 0)
            segmentsDir.mkdirs()
            val index = SegmentedVectorIndex(
                dimensions = dimensions,
                segmentsDir = segmentsDir,
                manifest = manifest,
                postingStore = postingStore,
                flushThreshold = flushThreshold,
                m = m,
                efConstruction = efConstruction,
                efSearch = efSearch,
                exactSearchThreshold = exactSearchThreshold,
            )
            index.loadFrozenSegments()
            return index
        }
    }

    private suspend fun loadFrozenSegments() {
        manifest.getActiveSegments().forEach { info ->
            val reader = SegmentReader.open(File(info.filePath))
            val segment = FrozenSegment(
                info = info,
                reader = reader,
                deletionBitmap = DeletionBitmap(info.vectorCount),
                localIndexToMediaId = LongArray(info.vectorCount) { -1L },
            )
            frozenSegments[info.id] = segment
            postingStore.getBySegment(info.id).forEach { posting ->
                if (posting.localIndex in segment.localIndexToMediaId.indices) {
                    segment.localIndexToMediaId[posting.localIndex] = posting.mediaItemId
                }
            }
            // Postings are the authoritative live set; deleted positions have no posting.
            segment.localIndexToMediaId.forEachIndexed { localIndex, mediaId ->
                if (mediaId < 0L) segment.deletionBitmap.markDeleted(localIndex)
            }
            manifest.updateDeletedCount(info.id, segment.deletionBitmap.deletedCount())
        }
    }

    override suspend fun add(id: Long, vector: FloatArray) {
        require(id >= 0L)
        require(vector.size == dimensions) { "Expected $dimensions dimensions, got ${vector.size}" }
        lock.write {
            kotlinx.coroutines.runBlocking {
                staging.add(id, vector)
                retireFrozenVersionsUnlocked(setOf(id))
                maybeFlushStagingUnlocked()
            }
        }
    }

    override suspend fun addBatch(entries: List<Pair<Long, FloatArray>>) {
        entries.forEach { (id, vector) ->
            require(id >= 0L)
            require(vector.size == dimensions) { "Expected $dimensions dimensions, got ${vector.size}" }
        }
        lock.write {
            kotlinx.coroutines.runBlocking {
                staging.addBatch(entries)
                retireFrozenVersionsUnlocked(entries.mapTo(mutableSetOf()) { it.first })
                maybeFlushStagingUnlocked()
            }
        }
    }

    private suspend fun retireFrozenVersionsUnlocked(ids: Set<Long>) {
        if (ids.isEmpty()) return
        frozenSegments.values.forEach { segment ->
            var changed = false
            segment.localIndexToMediaId.forEachIndexed { localIndex, mediaId ->
                if (mediaId in ids && !segment.deletionBitmap.isDeleted(localIndex)) {
                    segment.deletionBitmap.markDeleted(localIndex)
                    segment.localIndexToMediaId[localIndex] = -1L
                    changed = true
                }
            }
            if (changed) {
                manifest.updateDeletedCount(segment.info.id, segment.deletionBitmap.deletedCount())
            }
        }
        postingStore.deleteByMediaItems(ids.toList())
    }

    private suspend fun maybeFlushStagingUnlocked() {
        while (staging.size() >= flushThreshold) {
            flushStagingUnlocked()
        }
    }

    private suspend fun flushStagingUnlocked() {
        val entries = staging.exportEntries()
        if (entries.isEmpty()) return

        val tempSegmentId = nextSegmentId.incrementAndGet()
        val writtenInfo = SegmentWriter.write(
            outputDir = segmentsDir,
            segmentId = tempSegmentId,
            entries = entries,
            dimensions = dimensions,
            m = m,
            efConstruction = efConstruction,
        )
        val segmentId = manifest.addSegment(writtenInfo)
        val segmentInfo = writtenInfo.copy(id = segmentId)

        val localIndexToMediaId = LongArray(entries.size)
        entries.forEachIndexed { localIndex, (mediaId, _) ->
            localIndexToMediaId[localIndex] = mediaId
        }

        val reader = SegmentReader.open(File(segmentInfo.filePath))
        frozenSegments[segmentId] = FrozenSegment(
            info = segmentInfo,
            reader = reader,
            deletionBitmap = DeletionBitmap(entries.size),
            localIndexToMediaId = localIndexToMediaId,
        )

        val postings = entries.mapIndexed { localIndex, (mediaId, _) ->
            VectorPosting(
                mediaItemId = mediaId,
                segmentId = segmentId,
                localIndex = localIndex,
            )
        }
        postingStore.insertAll(postings)
        staging.clear()
    }

    override suspend fun search(query: FloatArray, topK: Int): List<SearchResult> {
        require(query.size == dimensions) { "Expected $dimensions dimensions, got ${query.size}" }
        if (topK <= 0) return emptyList()

        return lock.read {
            kotlinx.coroutines.runBlocking {
                if (size() <= exactSearchThreshold) {
                    val exact = LinearScanIndex(dimensions)
                    val entries = LinkedHashMap<Long, FloatArray>()
                    frozenSegments.values.forEach { segment ->
                        segment.localIndexToMediaId.forEachIndexed { localIndex, mediaId ->
                            if (mediaId >= 0L && !segment.deletionBitmap.isDeleted(localIndex)) {
                                entries[mediaId] = segment.reader.readVector(localIndex)
                            }
                        }
                    }
                    staging.exportEntries().forEach { (id, vector) -> entries[id] = vector }
                    exact.addBatch(entries.map { it.key to it.value })
                    exact.search(query, topK)
                } else {
                    val byId = LinkedHashMap<Long, SearchResult>()
                    frozenSegments.values.forEach { segment ->
                        SegmentHnswSearch.search(
                            reader = segment.reader,
                            deletionBitmap = segment.deletionBitmap,
                            query = query,
                            localIndexToMediaId = segment.localIndexToMediaId,
                            topK = topK,
                            efSearch = efSearch,
                        ).forEach { result -> byId[result.id] = result }
                    }
                    staging.search(query, topK).forEach { result -> byId[result.id] = result }
                    byId.values.sortedByDescending { it.score }.take(topK)
                }
            }
        }
    }

    override suspend fun getVector(id: Long): FloatArray? {
        return lock.read {
            kotlinx.coroutines.runBlocking {
                staging.getVector(id)?.let { return@runBlocking it }
                val posting = postingStore.getByMediaItem(id) ?: return@runBlocking null
                val segment = frozenSegments[posting.segmentId] ?: return@runBlocking null
                if (posting.localIndex !in segment.localIndexToMediaId.indices ||
                    segment.localIndexToMediaId[posting.localIndex] != id ||
                    segment.deletionBitmap.isDeleted(posting.localIndex)
                ) return@runBlocking null
                segment.reader.readVector(posting.localIndex)
            }
        }
    }

    override suspend fun remove(id: Long) {
        lock.write {
            kotlinx.coroutines.runBlocking {
                staging.remove(id)
                retireFrozenVersionsUnlocked(setOf(id))
            }
        }
    }

    override suspend fun contains(id: Long): Boolean = getVector(id) != null

    override fun size(): Int = lock.read {
        frozenSegments.values.sumOf { it.deletionBitmap.liveCount() } + staging.size()
    }

    override fun dimensions(): Int = dimensions

    override fun clear() {
        lock.write {
            kotlinx.coroutines.runBlocking {
                manifest.getActiveSegments().forEach { info ->
                    val ids = postingStore.getBySegment(info.id).map { it.mediaItemId }
                    postingStore.deleteByMediaItems(ids)
                    manifest.removeSegment(info.id)
                }
            }
            staging.clear()
            frozenSegments.values.forEach { it.reader.close() }
            frozenSegments.clear()
            segmentsDir.listFiles()?.forEach { it.delete() }
        }
    }

    override fun persist() {
        lock.write {
            kotlinx.coroutines.runBlocking {
                if (staging.size() > 0) {
                    flushStagingUnlocked()
                }
            }
        }
    }
}
