package com.recall.app.core.vector.segmented

import com.recall.app.core.vector.segment.InMemorySegmentManifest
import com.recall.app.core.vector.segment.InMemoryVectorPostingStore
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

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "segmented-index-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
