package com.recall.app.core.vector.distance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.sqrt

class VectorDistanceTest {
    @Test
    fun cosineSimilarity_identicalVectors_returnsOne() {
        val v = floatArrayOf(1f, 2f, 3f)
        assertEquals(1.0f, VectorDistance.cosineSimilarity(v, v), 1e-5f)
    }

    @Test
    fun cosineSimilarity_orthogonalVectors_returnsZero() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0.0f, VectorDistance.cosineSimilarity(a, b), 1e-5f)
    }

    @Test
    fun cosineSimilarity_oppositeVectors_returnsNegativeOne() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(-1f, 0f)
        assertEquals(-1.0f, VectorDistance.cosineSimilarity(a, b), 1e-5f)
    }

    @Test
    fun dotProduct_knownVectors() {
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(4f, 5f, 6f)
        assertEquals(32f, VectorDistance.dotProduct(a, b), 1e-5f)
    }

    @Test
    fun euclideanDistance_knownVectors() {
        val a = floatArrayOf(0f, 0f)
        val b = floatArrayOf(3f, 4f)
        assertEquals(5f, VectorDistance.euclideanDistance(a, b), 1e-5f)
    }

    @Test
    fun dimensionMismatch_throwsException() {
        val a = floatArrayOf(1f, 2f)
        val b = floatArrayOf(1f, 2f, 3f)
        assertThrows(IllegalArgumentException::class.java) {
            VectorDistance.cosineSimilarity(a, b)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VectorDistance.dotProduct(a, b)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VectorDistance.euclideanDistance(a, b)
        }
    }

    @Test
    fun cosineSimilarity_zeroVector_returnsZero() {
        val zero = floatArrayOf(0f, 0f)
        val unit = floatArrayOf(1f, 0f)
        assertEquals(0f, VectorDistance.cosineSimilarity(zero, unit), 1e-5f)
    }

    @Test
    fun euclideanDistance_identicalVectors_returnsZero() {
        val v = floatArrayOf(1f, sqrt(2f))
        assertEquals(0f, VectorDistance.euclideanDistance(v, v), 1e-5f)
    }
}
