package com.recall.app.core.vector.linear

import com.recall.app.core.vector.VectorIndexFactory
import com.recall.app.core.vector.distance.VectorDistance
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random

class LinearScanIndexTest {
    private lateinit var index: LinearScanIndex

    @Before
    fun setUp() {
        index = LinearScanIndex(dimensions = 4)
    }

    @Test
    fun addAndSearch_returnsCorrectResult() = runTest {
        val query = floatArrayOf(1f, 0f, 0f, 0f)
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.add(2L, floatArrayOf(0f, 1f, 0f, 0f))

        val results = index.search(query, topK = 1)
        assertEquals(1, results.size)
        assertEquals(1L, results[0].id)
        assertEquals(1.0f, results[0].score, 1e-5f)
        assertEquals(0f, results[0].distance, 1e-5f)
    }

    @Test
    fun search_returnsTopKInOrder() = runTest {
        val query = normalize(floatArrayOf(1f, 1f, 0f, 0f))
        index.add(1L, normalize(floatArrayOf(1f, 0f, 0f, 0f)))
        index.add(2L, normalize(floatArrayOf(1f, 1f, 0f, 0f)))
        index.add(3L, normalize(floatArrayOf(0f, 0f, 1f, 0f)))

        val results = index.search(query, topK = 2)
        assertEquals(2, results.size)
        assertEquals(2L, results[0].id)
        assertTrue(results[0].score >= results[1].score)
        assertEquals(1L, results[1].id)
    }

    @Test
    fun addBatch_worksCorrectly() = runTest {
        val entries = listOf(
            10L to floatArrayOf(1f, 0f, 0f, 0f),
            20L to floatArrayOf(0f, 1f, 0f, 0f),
        )
        index.addBatch(entries)

        assertEquals(2, index.size())
        assertTrue(index.contains(10L))
        assertTrue(index.contains(20L))
    }

    @Test
    fun getVector_returnsStoredVector() = runTest {
        val vector = floatArrayOf(1f, 0f, 0f, 0f)
        index.add(7L, vector)

        val retrieved = index.getVector(7L)
        assertEquals(4, retrieved?.size)
        for (i in vector.indices) {
            assertEquals(vector[i], retrieved!![i], 1e-5f)
        }
    }

    @Test
    fun getVector_nonExistentId_returnsNull() = runTest {
        assertEquals(null, index.getVector(999L))
    }

    @Test
    fun remove_removesVector() = runTest {
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.remove(1L)
        assertEquals(0, index.size())
        assertFalse(index.contains(1L))
    }

    @Test
    fun search_emptyIndex_returnsEmptyList() = runTest {
        val results = index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 5)
        assertTrue(results.isEmpty())
    }

    @Test
    fun contains_reflectsMembership() = runTest {
        assertFalse(index.contains(42L))
        index.add(42L, floatArrayOf(1f, 0f, 0f, 0f))
        assertTrue(index.contains(42L))
    }

    @Test
    fun size_reflectsVectorCount() = runTest {
        assertEquals(0, index.size())
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.add(2L, floatArrayOf(0f, 1f, 0f, 0f))
        assertEquals(2, index.size())
    }

    @Test
    fun dimensionMismatch_throwsException() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { index.add(1L, floatArrayOf(1f, 0f)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { index.search(floatArrayOf(1f, 0f), topK = 1) }
        }
    }

    @Test
    fun search_isDeterministic() = runTest {
        val query = normalize(floatArrayOf(3f, 1f, 2f, 0f))
        index.add(1L, normalize(floatArrayOf(1f, 0f, 0f, 0f)))
        index.add(2L, normalize(floatArrayOf(0f, 1f, 0f, 0f)))
        index.add(3L, normalize(floatArrayOf(0f, 0f, 1f, 0f)))

        val first = index.search(query, topK = 3)
        val second = index.search(query, topK = 3)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(first.map { it.score }, second.map { it.score })
    }

    @Test
    fun search_manyVectors_topOneIsCorrect() = runTest {
        val dimensions = 16
        val largeIndex = LinearScanIndex(dimensions)
        val random = Random(42)
        val query = randomNormalizedVector(random, dimensions)

        val entries = (1L..1000L).map { id ->
            id to randomNormalizedVector(random, dimensions)
        }
        largeIndex.addBatch(entries)

        val results = largeIndex.search(query, topK = 1)
        assertEquals(1, results.size)

        val expectedBest = entries.maxBy { (_, vector) ->
            VectorDistance.cosineSimilarity(query, vector)
        }
        assertEquals(expectedBest.first, results[0].id)
        assertEquals(
            VectorDistance.cosineSimilarity(query, expectedBest.second),
            results[0].score,
            1e-5f,
        )
    }

    @Test
    fun add_copiesVector_preventsExternalMutation() = runTest {
        val vector = floatArrayOf(1f, 0f, 0f, 0f)
        index.add(1L, vector)
        vector[0] = 0f

        val results = index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 1)
        assertEquals(1.0f, results[0].score, 1e-5f)
    }

    @Test
    fun factory_createsLinearScanIndex() {
        val factoryIndex = VectorIndexFactory.createLinearScan(8)
        assertEquals(8, factoryIndex.dimensions())
        assertTrue(factoryIndex is LinearScanIndex)
    }

    @Test
    fun clear_removesAllVectors() = runTest {
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.clear()
        assertEquals(0, index.size())
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.fold(0f) { acc, v -> acc + v * v })
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }

    private fun randomNormalizedVector(random: Random, dimensions: Int): FloatArray {
        val raw = FloatArray(dimensions) { random.nextFloat() * 2f - 1f }
        return normalize(raw)
    }
}
