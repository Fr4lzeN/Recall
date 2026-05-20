package com.recall.app.core.vector.benchmark

import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBenchmarkTest {

    @Test
    fun `linear scan 1K vectors search latency`() {
        runSearchBenchmark(
            label = "LinearScan",
            vectorCount = 1_000,
            buildIndex = { vectors ->
                val index = LinearScanIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 500L,
            targetP95Ms = 10L,
        )
    }

    @Test
    fun `linear scan 5K vectors search latency`() {
        runSearchBenchmark(
            label = "LinearScan",
            vectorCount = 5_000,
            buildIndex = { vectors ->
                val index = LinearScanIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 2_000L,
            targetP95Ms = 50L,
        )
    }

    @Test
    fun `linear scan 10K vectors search latency`() {
        runSearchBenchmark(
            label = "LinearScan",
            vectorCount = 10_000,
            buildIndex = { vectors ->
                val index = LinearScanIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 5_000L,
            targetP95Ms = 100L,
        )
    }

    @Test
    fun `hnsw 1K vectors search latency`() {
        runSearchBenchmark(
            label = "HNSW",
            vectorCount = 1_000,
            buildIndex = { vectors ->
                val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 200L,
            targetP95Ms = 50L,
        )
    }

    @Test
    fun `hnsw 5K vectors search latency`() {
        runSearchBenchmark(
            label = "HNSW",
            vectorCount = 5_000,
            buildIndex = { vectors ->
                val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 200L,
            targetP95Ms = 50L,
        )
    }

    @Test
    fun `hnsw 10K vectors search latency`() {
        runSearchBenchmark(
            label = "HNSW",
            vectorCount = 10_000,
            buildIndex = { vectors ->
                val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 200L,
            targetP95Ms = 50L,
        )
    }

    @Test
    fun `hnsw 50K vectors search latency`() {
        runSearchBenchmark(
            label = "HNSW",
            vectorCount = 50_000,
            buildIndex = { vectors ->
                val index = HnswIndex(BenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
            p95ThresholdMs = 200L,
            targetP95Ms = 50L,
        )
    }

    private fun runSearchBenchmark(
        label: String,
        vectorCount: Int,
        buildIndex: (List<Pair<Long, FloatArray>>) -> com.recall.app.core.vector.VectorIndex,
        p95ThresholdMs: Long,
        targetP95Ms: Long,
    ) {
        val vectors = BenchmarkUtils.generateVectors(vectorCount)
        val index = buildIndex(vectors)
        val queries = BenchmarkUtils.generateQueries(BenchmarkUtils.SEARCH_ITERATIONS)
        val latencies = BenchmarkUtils.measureSearchLatencies(index, queries)
        val stats = BenchmarkUtils.latencyStats(latencies)
        BenchmarkUtils.printLatencyTable(
            label = "$label search",
            vectorCount = vectorCount,
            stats = stats,
            thresholdMs = p95ThresholdMs,
        )
        println("Target (device): p95 < ${targetP95Ms}ms | CI threshold: p95 < ${p95ThresholdMs}ms")
        assertTrue(
            "$label $vectorCount p95=${stats.p95}ms exceeds CI threshold ${p95ThresholdMs}ms",
            stats.p95 < p95ThresholdMs,
        )
    }
}
