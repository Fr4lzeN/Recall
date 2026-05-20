package com.recall.app.core.vector.distance

import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class VectorDistanceEdgeCasesTest {
    @Test
    fun cosineSimilarity_bothZeroVectors_returnsZero() {
        val zero = FloatArray(16) { 0f }
        assertEquals(0f, VectorDistance.cosineSimilarity(zero, zero), 1e-5f)
    }

    @Test
    fun dotProduct_zeroVector_returnsZero() {
        val zero = FloatArray(8) { 0f }
        val unit = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        assertEquals(0f, VectorDistance.dotProduct(zero, unit), 1e-5f)
    }

    @Test
    fun euclideanDistance_zeroVectors_returnsZero() {
        val zero = FloatArray(32) { 0f }
        assertEquals(0f, VectorDistance.euclideanDistance(zero, zero), 1e-5f)
    }

    @Test
    fun cosineSimilarity_largeVectors_computesCorrectly() {
        val dimensions = 1000
        val a = FloatArray(dimensions) { i -> (i % 7).toFloat() }
        val b = FloatArray(dimensions) { i -> (i % 11).toFloat() }
        val expected = manualCosineSimilarity(a, b)
        assertEquals(expected, VectorDistance.cosineSimilarity(a, b), 1e-4f)
    }

    @Test
    fun dotProduct_largeVectors_computesCorrectly() {
        val dimensions = 1000
        val a = FloatArray(dimensions) { 1f }
        val b = FloatArray(dimensions) { 2f }
        assertEquals(dimensions * 2f, VectorDistance.dotProduct(a, b), 1e-3f)
    }

    @Test
    fun searchTenThousandVectors_completesUnderOneSecond() = runTest {
        val dimensions = 32
        val index = LinearScanIndex(dimensions)
        val random = Random(99)
        val entries = (1L..10_000L).map { id ->
            id to randomNormalizedVector(random, dimensions)
        }
        index.addBatch(entries)

        val query = randomNormalizedVector(random, dimensions)
        val elapsedMs = measureTimeMillis {
            val results = index.search(query, topK = 10)
            assertEquals(10, results.size)
        }
        assertTrue(
            "Search took ${elapsedMs}ms, expected < 1000ms",
            elapsedMs < 1000,
        )
    }

    private fun manualCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dot / denom else 0f
    }

    private fun randomNormalizedVector(random: Random, dimensions: Int): FloatArray {
        val raw = FloatArray(dimensions) { random.nextFloat() * 2f - 1f }
        val norm = sqrt(raw.fold(0f) { acc, v -> acc + v * v })
        return FloatArray(dimensions) { i -> raw[i] / norm }
    }
}
