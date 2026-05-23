package com.recall.app.core.vector.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceSearchBenchmarkTest {

    @Test
    fun deviceSearchLatencyBenchmark() {
        val rows = mutableListOf<DeviceBenchmarkUtils.SearchResultRow>()

        rows += runSearchBenchmark(
            indexType = "LinearScan",
            vectorCount = 1_000,
            buildIndex = { vectors ->
                val index = LinearScanIndex(DeviceBenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
        )
        rows += runSearchBenchmark(
            indexType = "LinearScan",
            vectorCount = 5_000,
            buildIndex = { vectors ->
                val index = LinearScanIndex(DeviceBenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
        )
        rows += runSearchBenchmark(
            indexType = "HNSW",
            vectorCount = 1_000,
            buildIndex = { vectors ->
                val index = HnswIndex(DeviceBenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
        )
        rows += runSearchBenchmark(
            indexType = "HNSW",
            vectorCount = 5_000,
            buildIndex = { vectors ->
                val index = HnswIndex(DeviceBenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
        )
        rows += runSearchBenchmark(
            indexType = "HNSW",
            vectorCount = 10_000,
            buildIndex = { vectors ->
                val index = HnswIndex(DeviceBenchmarkUtils.DIMENSIONS)
                runBlocking { index.addBatch(vectors) }
                index
            },
        )

        DeviceBenchmarkUtils.printSearchResultsTable(rows)

        rows.forEach { row ->
            assertTrue(
                "${row.indexType} ${row.vectorCount} produced no search timings",
                row.stats.mean >= 0.0,
            )
        }
    }

    private fun runSearchBenchmark(
        indexType: String,
        vectorCount: Int,
        buildIndex: (List<Pair<Long, FloatArray>>) -> VectorIndex,
    ): DeviceBenchmarkUtils.SearchResultRow {
        val vectors = DeviceBenchmarkUtils.generateVectors(vectorCount)
        val index = buildIndex(vectors)
        val queries = DeviceBenchmarkUtils.generateQueries(DeviceBenchmarkUtils.SEARCH_ITERATIONS)

        DeviceBenchmarkUtils.warmupSearch(index, queries)
        val latencies = DeviceBenchmarkUtils.measureSearchLatencies(index, queries)
        val stats = DeviceBenchmarkUtils.latencyStats(latencies)

        return DeviceBenchmarkUtils.SearchResultRow(
            indexType = indexType,
            vectorCount = vectorCount,
            stats = stats,
        )
    }
}
