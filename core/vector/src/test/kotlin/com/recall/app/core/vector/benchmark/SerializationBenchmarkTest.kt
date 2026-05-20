package com.recall.app.core.vector.benchmark

import com.recall.app.core.vector.hnsw.HnswIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationBenchmarkTest {

    @Test
    fun `serialize 10K vector index`() {
        val vectors = BenchmarkUtils.generateVectors(10_000)
        val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
        runBlocking { index.addBatch(vectors) }

        lateinit var data: ByteArray
        val elapsedMs = BenchmarkUtils.measureMs {
            data = index.serialize()
        }
        val sizeBytes = data.size.toLong()

        BenchmarkUtils.printTable(
            title = "HNSW serialize (10K vectors)",
            headers = listOf("operation", "elapsed_ms", "size"),
            rows = listOf(
                listOf("serialize", "$elapsedMs", BenchmarkUtils.formatBytes(sizeBytes)),
            ),
        )

        assertEquals(10_000, index.size())
        assertTrue("Serialized size ${sizeBytes}B too small", sizeBytes > 1_000_000L)
        assertTrue("Serialize took ${elapsedMs}ms", elapsedMs < 30_000L)
    }

    @Test
    fun `deserialize 10K vector index`() {
        val vectors = BenchmarkUtils.generateVectors(10_000)
        val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
        runBlocking { index.addBatch(vectors) }
        val data = index.serialize()

        var restored: HnswIndex? = null
        val elapsedMs = BenchmarkUtils.measureMs {
            restored = HnswIndex.deserialize(data, BenchmarkUtils.DIMENSIONS)
        }

        BenchmarkUtils.printTable(
            title = "HNSW deserialize (10K vectors)",
            headers = listOf("operation", "elapsed_ms", "input_size"),
            rows = listOf(
                listOf(
                    "deserialize",
                    "$elapsedMs",
                    BenchmarkUtils.formatBytes(data.size.toLong()),
                ),
            ),
        )

        assertEquals(10_000, restored!!.size())
        assertTrue("Deserialize took ${elapsedMs}ms", elapsedMs < 30_000L)
    }
}
