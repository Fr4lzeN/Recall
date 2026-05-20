package com.recall.app.core.vector.benchmark

import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexingBenchmarkTest {

    @Test
    fun `hnsw insert 10K vectors throughput`() {
        val vectors = BenchmarkUtils.generateVectors(10_000)
        val index = HnswIndex(BenchmarkUtils.DIMENSIONS)

        val elapsedMs = BenchmarkUtils.measureMs {
            runBlocking {
                vectors.forEach { (id, vector) -> index.add(id, vector) }
            }
        }

        val throughput = vectors.size.toDouble() / elapsedMs.coerceAtLeast(1) * 1000.0
        BenchmarkUtils.printTable(
            title = "HNSW single insert throughput (10K vectors)",
            headers = listOf("mode", "vectors", "elapsed_ms", "vectors_per_sec"),
            rows = listOf(
                listOf("single", "10000", "$elapsedMs", String.format("%.1f", throughput)),
            ),
        )
        assertTrue(index.size() == 10_000)
        assertTrue("HNSW single insert took ${elapsedMs}ms", elapsedMs < 120_000L)
    }

    @Test
    fun `hnsw batch insert 10K vectors throughput`() {
        val vectors = BenchmarkUtils.generateVectors(10_000)
        val index = HnswIndex(BenchmarkUtils.DIMENSIONS)

        val elapsedMs = BenchmarkUtils.measureMs {
            runBlocking { index.addBatch(vectors) }
        }

        val throughput = vectors.size.toDouble() / elapsedMs.coerceAtLeast(1) * 1000.0
        BenchmarkUtils.printTable(
            title = "HNSW batch insert throughput (10K vectors)",
            headers = listOf("mode", "vectors", "elapsed_ms", "vectors_per_sec"),
            rows = listOf(
                listOf("batch", "10000", "$elapsedMs", String.format("%.1f", throughput)),
            ),
        )
        assertTrue(index.size() == 10_000)
        assertTrue("HNSW batch insert took ${elapsedMs}ms", elapsedMs < 60_000L)
    }

    @Test
    fun `linear scan insert 10K vectors throughput`() {
        val vectors = BenchmarkUtils.generateVectors(10_000)
        val index = LinearScanIndex(BenchmarkUtils.DIMENSIONS)

        val elapsedMs = BenchmarkUtils.measureMs {
            runBlocking { index.addBatch(vectors) }
        }

        val throughput = vectors.size.toDouble() / elapsedMs.coerceAtLeast(1) * 1000.0
        BenchmarkUtils.printTable(
            title = "LinearScan batch insert throughput (10K vectors)",
            headers = listOf("mode", "vectors", "elapsed_ms", "vectors_per_sec"),
            rows = listOf(
                listOf("batch", "10000", "$elapsedMs", String.format("%.1f", throughput)),
            ),
        )
        assertTrue(index.size() == 10_000)
        assertTrue("LinearScan insert took ${elapsedMs}ms", elapsedMs < 10_000L)
    }
}
