package com.recall.app.core.vector.segmented

import com.recall.app.core.vector.segment.InMemorySegmentManifest
import com.recall.app.core.vector.segment.InMemoryVectorPostingStore
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SegmentedVectorIndexTest {

    @Test
    fun flushAndSearch_acrossFrozenSegments() = runTest {
        val dir = createTempDir()
        val manifest = InMemorySegmentManifest()
        val postings = InMemoryVectorPostingStore()
        val index = SegmentedVectorIndex.open(
            dimensions = 4,
            segmentsDir = dir,
            manifest = manifest,
            postingStore = postings,
            flushThreshold = 2,
        )

        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.add(2L, floatArrayOf(0f, 1f, 0f, 0f))
        index.add(3L, floatArrayOf(0f, 0f, 1f, 0f))

        assertEquals(1, manifest.getActiveSegments().size)
        assertEquals(3, index.size())

        index.add(4L, floatArrayOf(0f, 0f, 0f, 1f))
        val results = index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 2)
        assertTrue(results.any { it.id == 1L })
        assertEquals(2, results.size)

        dir.deleteRecursively()
    }

    @Test
    fun getVector_returnsVectorFromStagingAndFrozenSegments() = runTest {
        val dir = createTempDir()
        val manifest = InMemorySegmentManifest()
        val postings = InMemoryVectorPostingStore()
        val index = SegmentedVectorIndex.open(
            dimensions = 4,
            segmentsDir = dir,
            manifest = manifest,
            postingStore = postings,
            flushThreshold = 2,
        )

        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        val stagingVector = index.getVector(1L)
        assertEquals(1f, stagingVector!![0], 1e-5f)

        index.add(2L, floatArrayOf(0f, 1f, 0f, 0f))
        val frozenVector = index.getVector(1L)
        assertEquals(1f, frozenVector!![0], 1e-5f)
        assertEquals(null, index.getVector(999L))

        dir.deleteRecursively()
    }

    @Test
    fun remove_marksDeletionInFrozenSegment() = runTest {
        val dir = createTempDir()
        val manifest = InMemorySegmentManifest()
        val postings = InMemoryVectorPostingStore()
        val index = SegmentedVectorIndex.open(
            dimensions = 4,
            segmentsDir = dir,
            manifest = manifest,
            postingStore = postings,
            flushThreshold = 2,
        )

        index.add(10L, floatArrayOf(1f, 0f, 0f, 0f))
        index.add(20L, floatArrayOf(0f, 1f, 0f, 0f))
        index.remove(10L)

        assertFalse(index.contains(10L))
        val results = index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 5)
        assertTrue(results.none { it.id == 10L })

        val segment = manifest.getActiveSegments().first()
        assertTrue(segment.deletedCount >= 1)

        dir.deleteRecursively()
    }

    @Test
    fun removeAndReopen_restoresLiveCountAndDoesNotReturnGhosts() = runTest {
        for (threshold in listOf(0, 2000)) {
            val dir = createTempDir()
            val manifest = InMemorySegmentManifest()
            val postings = InMemoryVectorPostingStore()
            val index = SegmentedVectorIndex.open(3, dir, manifest, postings, exactSearchThreshold = threshold)
            index.addBatch(listOf(
                1L to floatArrayOf(1f, 0f, 0f),
                2L to floatArrayOf(0f, 1f, 0f),
                3L to floatArrayOf(0f, 0f, 1f),
            ))
            index.persist()
            index.remove(1L)
            val reopened = SegmentedVectorIndex.open(3, dir, manifest, postings, exactSearchThreshold = threshold)
            assertEquals(2, reopened.size())
            assertFalse(reopened.contains(1L))
            val hits = reopened.search(floatArrayOf(1f, 0f, 0f), 2)
            assertEquals(setOf(2L, 3L), hits.map { it.id }.toSet())
            assertEquals(2, hits.size)
            dir.deleteRecursively()
        }
    }

    @Test
    fun replacingFrozenVector_supersedesOldScoreAndSurvivesReopen() = runTest {
        for (threshold in listOf(0, 2000)) {
            val dir = createTempDir()
            val manifest = InMemorySegmentManifest()
            val postings = InMemoryVectorPostingStore()
            val index = SegmentedVectorIndex.open(3, dir, manifest, postings, exactSearchThreshold = threshold)
            index.addBatch(listOf(1L to floatArrayOf(1f, 0f, 0f), 2L to floatArrayOf(0f, 1f, 0f)))
            index.persist()
            index.add(1L, floatArrayOf(0f, 0f, 1f))
            assertEquals(2, index.size())
            val hits = index.search(floatArrayOf(1f, 0f, 0f), 10)
            assertEquals(2, hits.size)
            assertEquals(setOf(1L, 2L), hits.map { it.id }.toSet())
            assertEquals(0f, hits.first { it.id == 1L }.score, 1e-6f)
            index.persist()
            val reopened = SegmentedVectorIndex.open(3, dir, manifest, postings, exactSearchThreshold = threshold)
            assertEquals(2, reopened.size())
            assertEquals(listOf(1L), reopened.search(floatArrayOf(0f, 0f, 1f), 1).map { it.id })
            dir.deleteRecursively()
        }
    }

    @Test
    fun smallCollection_matchesExactScanAcrossSegmentsAndStaging() = runTest {
        val dir = createTempDir()
        val index = SegmentedVectorIndex.open(
            8, dir, InMemorySegmentManifest(), InMemoryVectorPostingStore(), flushThreshold = 80,
            m = 2, efConstruction = 5, efSearch = 1,
        )
        val exact = LinearScanIndex(8)
        val random = kotlin.random.Random(17)
        val entries = List(600) { id -> id.toLong() to FloatArray(8) { random.nextFloat() - 0.5f } }
        index.addBatch(entries.take(500))
        index.addBatch(entries.drop(500).take(80))
        index.addBatch(entries.takeLast(20))
        exact.addBatch(entries)
        repeat(20) {
            val query = FloatArray(8) { random.nextFloat() - 0.5f }
            assertEquals(exact.search(query, 10).map { it.id }, index.search(query, 10).map { it.id })
        }
        dir.deleteRecursively()
    }

    @Test
    fun clear_removesPersistedPostingsAndSegments() = runTest {
        val dir = createTempDir()
        val manifest = InMemorySegmentManifest()
        val postings = InMemoryVectorPostingStore()
        val index = SegmentedVectorIndex.open(2, dir, manifest, postings)
        index.add(1L, floatArrayOf(1f, 0f))
        index.persist()
        index.clear()
        assertEquals(null, postings.getByMediaItem(1L))
        assertTrue(manifest.getActiveSegments().isEmpty())
        val reopened = SegmentedVectorIndex.open(2, dir, manifest, postings)
        assertEquals(0, reopened.size())
        assertFalse(reopened.contains(1L))
        dir.deleteRecursively()
    }

    @Test
    fun diagnosticSnapshotIsDetachedBoundedAndTracksLiveRevisions() = runTest {
        val dir = createTempDir()
        val index = SegmentedVectorIndex.open(2, dir, InMemorySegmentManifest(), InMemoryVectorPostingStore())
        index.add(1L, floatArrayOf(1f, 0f))
        index.persist()
        index.add(2L, floatArrayOf(0f, 1f))
        val snapshot = index.diagnosticSnapshot(2)!!
        assertEquals(setOf(1L, 2L), snapshot.entries.map { it.first }.toSet())
        assertEquals("exact_linear_scan", snapshot.searchMethod)
        assertEquals(snapshot.revision, index.diagnosticRevision())
        assertEquals(null, index.diagnosticSnapshot(1))
        snapshot.entries.first { it.first == 2L }.second[0] = 99f
        assertEquals(0f, index.getVector(2L)!![0], 0f)
        index.remove(1L)
        assertTrue(index.diagnosticRevision()!! > snapshot.revision)
        assertEquals(listOf(2L), index.diagnosticSnapshot(2)!!.entries.map { it.first })
        index.clear()
        assertTrue(index.diagnosticSnapshot(2)!!.entries.isEmpty())
        dir.deleteRecursively()
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "segmented-index-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
