package com.recall.app.core.vector.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recall.app.core.vector.hnsw.HnswIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceIndexingBenchmarkTest {

    @Test
    fun deviceIndexingBenchmark() {
        val vectorCount = 10_000
        val rows = mutableListOf<DeviceBenchmarkUtils.IndexingResultRow>()

        rows += measureSingleAddBenchmark("LinearScan", vectorCount)
        rows += measureBatchAddBenchmark("LinearScan", vectorCount)
        rows += measureSingleAddBenchmark("HNSW", vectorCount)
        rows += measureBatchAddBenchmark("HNSW", vectorCount)

        DeviceBenchmarkUtils.printIndexingResultsTable(rows)

        rows.forEach { row ->
            if (row.mode == "single") {
                assertTrue(
                    "${row.indexType} single add produced no timings",
                    row.stats != null,
                )
            } else {
                assertTrue(
                    "${row.indexType} batch add produced no throughput",
                    row.throughput?.let { it > 0.0 } == true,
                )
            }
        }
    }

    private fun measureSingleAddBenchmark(
        indexType: String,
        vectorCount: Int,
    ): DeviceBenchmarkUtils.IndexingResultRow {
        val vectors = DeviceBenchmarkUtils.generateVectors(
            count = DeviceBenchmarkUtils.WARMUP_ITERATIONS + DeviceBenchmarkUtils.SINGLE_ADD_ITERATIONS,
            seed = when (indexType) {
                "LinearScan" -> DeviceBenchmarkUtils.DEFAULT_SEED
                "HNSW" -> DeviceBenchmarkUtils.DEFAULT_SEED + 10
                else -> DeviceBenchmarkUtils.DEFAULT_SEED
            },
        )
        val index = when (indexType) {
            "LinearScan" -> LinearScanIndex(DeviceBenchmarkUtils.DIMENSIONS)
            "HNSW" -> HnswIndex(DeviceBenchmarkUtils.DIMENSIONS)
            else -> error("Unknown index type: $indexType")
        }

        vectors.take(DeviceBenchmarkUtils.WARMUP_ITERATIONS).forEach { (id, vector) ->
            runBlocking { index.add(id, vector) }
        }

        val measuredVectors = vectors.drop(DeviceBenchmarkUtils.WARMUP_ITERATIONS)
        val latencies = DeviceBenchmarkUtils.measureSingleAddLatencies(index, measuredVectors)
        val stats = DeviceBenchmarkUtils.latencyStats(latencies)

        return DeviceBenchmarkUtils.IndexingResultRow(
            indexType = indexType,
            mode = "single",
            vectorCount = measuredVectors.size,
            stats = stats,
        )
    }

    private fun measureBatchAddBenchmark(
        indexType: String,
        vectorCount: Int,
    ): DeviceBenchmarkUtils.IndexingResultRow {
        val vectors = DeviceBenchmarkUtils.generateVectors(vectorCount)
        val index = when (indexType) {
            "LinearScan" -> LinearScanIndex(DeviceBenchmarkUtils.DIMENSIONS)
            "HNSW" -> HnswIndex(DeviceBenchmarkUtils.DIMENSIONS)
            else -> error("Unknown index type: $indexType")
        }

        val elapsedMs = DeviceBenchmarkUtils.measureMs {
            runBlocking { index.addBatch(vectors) }
        }
        val throughput = vectors.size / elapsedMs.coerceAtLeast(0.001) * 1000.0

        assertTrue(index.size() == vectorCount)

        return DeviceBenchmarkUtils.IndexingResultRow(
            indexType = indexType,
            mode = "batch",
            vectorCount = vectorCount,
            throughput = throughput,
        )
    }
}
