package com.recall.app.core.vector.segment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SegmentWriterReaderTest {

    @Test
    fun writeAndRead_vectorsAndNeighborsMatch() = runTest {
        val dir = createTempDir()
        val entries = listOf(
            1L to floatArrayOf(1f, 0f, 0f, 0f),
            2L to floatArrayOf(0f, 1f, 0f, 0f),
            3L to floatArrayOf(0f, 0f, 1f, 0f),
        )

        val info = SegmentWriter.write(
            outputDir = dir,
            segmentId = 42L,
            entries = entries,
            dimensions = 4,
        )

        SegmentReader.open(File(info.filePath)).use { reader ->
            assertEquals(3, reader.vectorCount)
            assertEquals(4, reader.dimensions)
            assertArrayEquals(entries[0].second, reader.readVector(0), 1e-5f)
            assertArrayEquals(entries[1].second, reader.readVector(1), 1e-5f)
            assertArrayEquals(entries[2].second, reader.readVector(2), 1e-5f)
            assertEquals(0, reader.readNodeLevel(0))
            assertTrue(reader.readNeighbors(0, 0).isNotEmpty() || reader.vectorCount == 1)
        }

        dir.deleteRecursively()
    }

    private fun createTempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "segment-wr-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
