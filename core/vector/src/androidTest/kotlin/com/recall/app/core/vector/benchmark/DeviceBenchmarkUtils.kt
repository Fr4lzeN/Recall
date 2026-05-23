package com.recall.app.core.vector.benchmark

import android.util.Log
import com.recall.app.core.vector.VectorIndex
import kotlinx.coroutines.runBlocking
import java.util.Random
import kotlin.math.sqrt

object DeviceBenchmarkUtils {
    private const val LOG_TAG = "RecallBenchmark"
    const val DIMENSIONS = 384
    const val TOP_K = 10
    const val SEARCH_ITERATIONS = 100
    const val WARMUP_ITERATIONS = 5
    const val SINGLE_ADD_ITERATIONS = 100
    const val DEFAULT_SEED = 42

    fun randomNormalizedVector(dimensions: Int, random: Random): FloatArray {
        val v = FloatArray(dimensions) { random.nextFloat() * 2f - 1f }
        val norm = sqrt(v.fold(0f) { acc, x -> acc + x * x })
        for (i in v.indices) v[i] /= norm
        return v
    }

    fun measureMs(block: () -> Unit): Double {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000.0
    }

    fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val index = (p / 100.0 * (sorted.size - 1)).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    fun generateVectors(
        count: Int,
        dimensions: Int = DIMENSIONS,
        seed: Int = DEFAULT_SEED,
    ): List<Pair<Long, FloatArray>> {
        val random = Random(seed.toLong())
        return (0L until count).map { id -> id to randomNormalizedVector(dimensions, random) }
    }

    fun generateQueries(
        count: Int,
        dimensions: Int = DIMENSIONS,
        seed: Int = DEFAULT_SEED,
    ): List<FloatArray> {
        val random = Random((seed + 1).toLong())
        return List(count) { randomNormalizedVector(dimensions, random) }
    }

    fun latencyStats(latenciesMs: List<Double>): LatencyStats {
        val sorted = latenciesMs.sorted()
        val mean = sorted.average()
        return LatencyStats(
            p50 = percentile(sorted, 50.0),
            p95 = percentile(sorted, 95.0),
            mean = mean,
        )
    }

    fun warmupSearch(index: VectorIndex, queries: List<FloatArray>, topK: Int = TOP_K) {
        queries.take(WARMUP_ITERATIONS).forEach { query ->
            runBlocking { index.search(query, topK) }
        }
    }

    fun measureSearchLatencies(
        index: VectorIndex,
        queries: List<FloatArray>,
        topK: Int = TOP_K,
    ): List<Double> {
        return queries.map { query ->
            measureMs { runBlocking { index.search(query, topK) } }
        }
    }

    fun measureSingleAddLatencies(
        index: VectorIndex,
        vectors: List<Pair<Long, FloatArray>>,
    ): List<Double> {
        return vectors.map { (id, vector) ->
            measureMs { runBlocking { index.add(id, vector) } }
        }
    }

    private fun logLine(line: String) {
        println(line)
        Log.i(LOG_TAG, line)
    }

    fun printSearchResultsTable(rows: List<SearchResultRow>) {
        logLine("")
        logLine("=== Device Search Benchmark ===")
        logLine("| Index Type  | Vectors | Avg (ms) | P50 (ms) | P95 (ms) |")
        logLine("|-------------|---------|----------|----------|----------|")
        rows.forEach { row ->
            logLine(
                String.format(
                    "| %-11s | %7s | %8.2f | %8.2f | %8.2f |",
                    row.indexType,
                    "%,d".format(row.vectorCount),
                    row.stats.mean,
                    row.stats.p50,
                    row.stats.p95,
                ),
            )
        }
    }

    fun printIndexingResultsTable(rows: List<IndexingResultRow>) {
        logLine("")
        logLine("=== Device Indexing Benchmark ===")
        logLine("| Index Type  | Mode   | Vectors | Avg (ms) | P50 (ms) | P95 (ms) | Throughput (vec/s) |")
        logLine("|-------------|--------|---------|----------|----------|----------|--------------------|")
        rows.forEach { row ->
            val throughput = row.throughput?.let { String.format("%.1f", it) } ?: "-"
            val avg = row.stats?.mean?.let { String.format("%.2f", it) } ?: "-"
            val p50 = row.stats?.p50?.let { String.format("%.2f", it) } ?: "-"
            val p95 = row.stats?.p95?.let { String.format("%.2f", it) } ?: "-"
            logLine(
                String.format(
                    "| %-11s | %-6s | %7s | %8s | %8s | %8s | %18s |",
                    row.indexType,
                    row.mode,
                    "%,d".format(row.vectorCount),
                    avg,
                    p50,
                    p95,
                    throughput,
                ),
            )
        }
    }

    data class LatencyStats(
        val p50: Double,
        val p95: Double,
        val mean: Double,
    )

    data class SearchResultRow(
        val indexType: String,
        val vectorCount: Int,
        val stats: LatencyStats,
    )

    data class IndexingResultRow(
        val indexType: String,
        val mode: String,
        val vectorCount: Int,
        val stats: LatencyStats? = null,
        val throughput: Double? = null,
    )
}
