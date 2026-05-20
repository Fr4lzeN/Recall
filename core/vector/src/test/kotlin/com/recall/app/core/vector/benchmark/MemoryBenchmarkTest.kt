package com.recall.app.core.vector.benchmark

import com.recall.app.core.vector.hnsw.HnswIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryBenchmarkTest {

    @Test
    fun `hnsw memory usage for 10K vectors`() {
        measureHnswMemory(vectorCount = 10_000)
    }

    @Test
    fun `hnsw memory usage for 50K vectors`() {
        measureHnswMemory(vectorCount = 50_000)
    }

    private fun measureHnswMemory(vectorCount: Int) {
        System.gc()
        val beforeBytes = BenchmarkUtils.usedMemoryBytes()

        val vectors = BenchmarkUtils.generateVectors(vectorCount)
        val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
        runBlocking { index.addBatch(vectors) }

        System.gc()
        val afterBytes = BenchmarkUtils.usedMemoryBytes()
        val deltaBytes = (afterBytes - beforeBytes).coerceAtLeast(0L)

        val vectorStorage = BenchmarkUtils.estimateVectorStorageBytes(vectorCount)
        val graphOverhead = (deltaBytes - vectorStorage).coerceAtLeast(0L)
        val overheadRatio = if (vectorStorage > 0) graphOverhead.toDouble() / vectorStorage else 0.0

        BenchmarkUtils.printTable(
            title = "HNSW memory estimate ($vectorCount vectors, ${BenchmarkUtils.DIMENSIONS}d)",
            headers = listOf("metric", "value"),
            rows = listOf(
                listOf("heap_delta", BenchmarkUtils.formatBytes(deltaBytes)),
                listOf("vector_storage_est", BenchmarkUtils.formatBytes(vectorStorage)),
                listOf("graph_overhead_est", BenchmarkUtils.formatBytes(graphOverhead)),
                listOf("overhead_ratio", String.format("%.2fx", 1.0 + overheadRatio)),
                listOf("index_size", index.size().toString()),
            ),
        )

        assertTrue(index.size() == vectorCount)
        assertTrue(
            "Measured heap delta ${BenchmarkUtils.formatBytes(deltaBytes)} below vector storage estimate",
            deltaBytes >= vectorStorage / 4,
        )
    }
}
