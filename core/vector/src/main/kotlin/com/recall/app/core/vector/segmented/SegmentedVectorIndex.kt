package com.recall.app.core.vector.segmented

import com.recall.app.core.vector.PersistableVectorIndex
import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.bitmap.DeletionBitmap
import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.segment.SegmentHnswSearch
import com.recall.app.core.vector.segment.SegmentInfo
import com.recall.app.core.vector.segment.SegmentManifest
import com.recall.app.core.vector.segment.SegmentReader
import com.recall.app.core.vector.segment.SegmentWriter
import com.recall.app.core.vector.segment.VectorPosting
import com.recall.app.core.vector.segment.VectorPostingStore
import java.io.File
import java.util.PriorityQueue
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
        ): SegmentedVectorIndex {
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
        }
    }

    override suspend fun add(id: Long, vector: FloatArray) {
        lock.write {
            kotlinx.coroutines.runBlocking {
                staging.add(id, vector)
                maybeFlushStagingUnlocked()
            }
        }
    }

    override suspend fun addBatch(entries: List<Pair<Long, FloatArray>>) {
        lock.write {
            kotlinx.coroutines.runBlocking {
                staging.addBatch(entries)
                maybeFlushStagingUnlocked()
            }
        }
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

        val frozenResults = lock.read {
            val merged = PriorityQueue<SearchResult>(compareByDescending { it.distance })
            frozenSegments.values.forEach { segment ->
                SegmentHnswSearch.search(
                    reader = segment.reader,
                    deletionBitmap = segment.deletionBitmap,
                    query = query,
                    localIndexToMediaId = segment.localIndexToMediaId,
                    topK = topK,
                    efSearch = efSearch,
                ).forEach { result -> addToTopK(merged, result, topK) }
            }
            merged
        }

        val stagingResults = lock.read {
            kotlinx.coroutines.runBlocking { staging.search(query, topK) }
        }

        val merged = PriorityQueue<SearchResult>(compareByDescending { it.distance })
        frozenResults.forEach { addToTopK(merged, it, topK) }
        stagingResults.forEach { addToTopK(merged, it, topK) }
        return merged.sortedByDescending { it.score }
    }

    private fun addToTopK(heap: PriorityQueue<SearchResult>, result: SearchResult, topK: Int) {
        heap.add(result)
        if (heap.size > topK) {
            heap.poll()
        }
    }

    override suspend fun remove(id: Long) {
        lock.write {
            if (staging.contains(id)) {
                staging.remove(id)
                postingStore.deleteByMediaItems(listOf(id))
                return@write
            }

            val posting = postingStore.getByMediaItem(id) ?: return@write
            val segment = frozenSegments[posting.segmentId] ?: return@write
            segment.deletionBitmap.markDeleted(posting.localIndex)
            manifest.updateDeletedCount(
                segmentId = posting.segmentId,
                deletedCount = segment.deletionBitmap.deletedCount(),
            )
            postingStore.deleteByMediaItems(listOf(id))
        }
    }

    override suspend fun contains(id: Long): Boolean = lock.read {
        staging.contains(id) || postingStore.getByMediaItem(id) != null
    }

    override fun size(): Int = lock.read {
        frozenSegments.values.sumOf { it.deletionBitmap.liveCount() } + staging.size()
    }

    override fun dimensions(): Int = dimensions

    override fun clear() {
        lock.write {
            staging.clear()
            frozenSegments.values.forEach { it.reader.close() }
            frozenSegments.clear()
            segmentsDir.listFiles()?.forEach { it.delete() }
            kotlinx.coroutines.runBlocking {
                manifest.getActiveSegments().forEach { manifest.removeSegment(it.id) }
            }
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
