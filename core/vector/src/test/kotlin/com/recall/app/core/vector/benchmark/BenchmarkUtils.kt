package com.recall.app.core.vector.benchmark

import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import java.util.Random
import kotlin.math.sqrt

object BenchmarkUtils {
    const val DIMENSIONS = 384
    const val TOP_K = 10
    const val SEARCH_ITERATIONS = 100
    const val DEFAULT_SEED = 42

    fun randomNormalizedVector(dimensions: Int, random: Random): FloatArray {
        val v = FloatArray(dimensions) { random.nextFloat() * 2f - 1f }
        val norm = sqrt(v.fold(0f) { acc, x -> acc + x * x })
        for (i in v.indices) v[i] /= norm
        return v
    }

    fun measureMs(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }

    fun percentile(sorted: List<Long>, p: Double): Long {
        if (sorted.isEmpty()) return 0L
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

    fun latencyStats(latenciesMs: List<Long>): LatencyStats {
        val sorted = latenciesMs.sorted()
        val mean = sorted.average()
        return LatencyStats(
            p50 = percentile(sorted, 50.0),
            p95 = percentile(sorted, 95.0),
            p99 = percentile(sorted, 99.0),
            mean = mean,
            min = sorted.firstOrNull() ?: 0L,
            max = sorted.lastOrNull() ?: 0L,
        )
    }

    fun printLatencyTable(label: String, vectorCount: Int, stats: LatencyStats, thresholdMs: Long? = null) {
        val thresholdNote = thresholdMs?.let { " (threshold p95 < ${it}ms)" }.orEmpty()
        println()
        println("=== $label [$vectorCount vectors, $SEARCH_ITERATIONS searches]$thresholdNote ===")
        println(String.format("%-8s %8s %8s %8s %10s", "metric", "p50", "p95", "p99", "mean"))
        println(String.format("%-8s %7dms %7dms %7dms %9.2fms", "latency", stats.p50, stats.p95, stats.p99, stats.mean))
        println(String.format("min=%dms max=%dms", stats.min, stats.max))
    }

    fun printTable(title: String, headers: List<String>, rows: List<List<String>>) {
        println()
        println("=== $title ===")
        val widths = headers.indices.map { col ->
            maxOf(headers[col].length, rows.maxOfOrNull { it.getOrElse(col) { "" }.length } ?: 0)
        }
        fun row(cells: List<String>) {
            println(cells.mapIndexed { i, cell -> cell.padEnd(widths[i]) }.joinToString("  "))
        }
        row(headers)
        row(widths.map { "-".repeat(it) })
        rows.forEach { row(it) }
    }

    fun measureSearchLatencies(
        index: VectorIndex,
        queries: List<FloatArray>,
        topK: Int = TOP_K,
    ): List<Long> {
        return queries.map { query ->
            measureMs { runBlocking { index.search(query, topK) } }
        }
    }

    fun recallAtK(
        approximate: VectorIndex,
        groundTruth: VectorIndex,
        queries: List<FloatArray>,
        topK: Int = TOP_K,
    ): Double {
        if (queries.isEmpty()) return 0.0
        var total = 0.0
        for (query in queries) {
            val approxIds = runBlocking { approximate.search(query, topK) }.map { it.id }.toSet()
            val exactIds = runBlocking { groundTruth.search(query, topK) }.map { it.id }.toSet()
            total += approxIds.intersect(exactIds).size.toDouble() / topK
        }
        return total / queries.size
    }

    fun buildGroundTruthIndex(vectors: List<Pair<Long, FloatArray>>): LinearScanIndex {
        val index = LinearScanIndex(DIMENSIONS)
        runBlocking { index.addBatch(vectors) }
        return index
    }

    fun usedMemoryBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
    }

    fun estimateVectorStorageBytes(vectorCount: Int, dimensions: Int = DIMENSIONS): Long {
        return vectorCount.toLong() * dimensions * 4L
    }

    data class LatencyStats(
        val p50: Long,
        val p95: Long,
        val p99: Long,
        val mean: Double,
        val min: Long,
        val max: Long,
    )
}
