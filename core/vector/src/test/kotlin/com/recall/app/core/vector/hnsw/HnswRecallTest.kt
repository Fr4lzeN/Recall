package com.recall.app.core.vector.hnsw

import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.sqrt

class HnswRecallTest {

    @Test
    fun recall_at_10_is_at_least_95_percent_for_5000_vectors() {
        val dimensions = 128
        val vectorCount = 5000
        val queryCount = 100
        val topK = 10

        val random = Random(42)
        val vectors = (0L until vectorCount).map { id ->
            id to randomNormalizedVector(dimensions, random)
        }

        val hnswIndex = HnswIndex(dimensions)
        runBlocking { hnswIndex.addBatch(vectors) }

        val linearIndex = LinearScanIndex(dimensions)
        runBlocking { linearIndex.addBatch(vectors) }

        var totalRecall = 0.0
        val queries = (0 until queryCount).map { randomNormalizedVector(dimensions, random) }

        for (query in queries) {
            val hnswResults = runBlocking { hnswIndex.search(query, topK) }.map { it.id }.toSet()
            val linearResults = runBlocking { linearIndex.search(query, topK) }.map { it.id }.toSet()
            val overlap = hnswResults.intersect(linearResults).size
            totalRecall += overlap.toDouble() / topK
        }

        val avgRecall = totalRecall / queryCount
        assertTrue("Recall@$topK = $avgRecall, expected >= 0.95", avgRecall >= 0.95)
    }

    @Test
    fun search_latency_under_50ms_for_10000_vectors() {
        val dimensions = 384
        val vectorCount = 10000
        val queryCount = 50
        val topK = 10

        val random = Random(7)
        val vectors = (0L until vectorCount).map { id ->
            id to randomNormalizedVector(dimensions, random)
        }

        val hnswIndex = HnswIndex(dimensions)
        runBlocking { hnswIndex.addBatch(vectors) }

        val queries = (0 until queryCount).map { randomNormalizedVector(dimensions, random) }
        val latencies = LongArray(queryCount)
        for (i in queries.indices) {
            val start = System.nanoTime()
            runBlocking { hnswIndex.search(queries[i], topK) }
            latencies[i] = System.nanoTime() - start
        }
        latencies.sort()
        val p95Index = (queryCount * 0.95).toInt().coerceAtMost(queryCount - 1)
        val p95Ms = latencies[p95Index] / 1_000_000.0
        assertTrue("p95 search latency ${p95Ms}ms, expected < 50ms", p95Ms < 50.0)
    }

    private fun randomNormalizedVector(dimensions: Int, random: Random): FloatArray {
        val raw = FloatArray(dimensions) { random.nextFloat() * 2f - 1f }
        val norm = sqrt(raw.fold(0f) { acc, v -> acc + v * v })
        return FloatArray(dimensions) { i -> raw[i] / norm }
    }
}
