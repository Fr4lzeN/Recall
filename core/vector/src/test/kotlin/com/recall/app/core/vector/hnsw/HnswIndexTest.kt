package com.recall.app.core.vector.hnsw

import com.recall.app.core.vector.VectorIndexFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class HnswIndexTest {
    private lateinit var index: HnswIndex

    @Before
    fun setUp() {
        index = HnswIndex(dimensions = 4)
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
    fun remove_removesVectorFromResults() = runTest {
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.add(2L, floatArrayOf(0f, 1f, 0f, 0f))
        index.remove(1L)

        assertEquals(1, index.size())
        assertFalse(index.contains(1L))
        val results = index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 5)
        assertTrue(results.none { it.id == 1L })
    }

    @Test
    fun contains_returnsCorrectValues() = runTest {
        assertFalse(index.contains(42L))
        index.add(42L, floatArrayOf(1f, 0f, 0f, 0f))
        assertTrue(index.contains(42L))
    }

    @Test
    fun search_emptyIndex_returnsEmpty() = runTest {
        val results = index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 5)
        assertTrue(results.isEmpty())
    }

    @Test
    fun dimensionMismatch_throws() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { index.add(1L, floatArrayOf(1f, 0f)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { index.search(floatArrayOf(1f, 0f), topK = 1) }
        }
    }

    @Test
    fun size_tracksCorrectly() = runTest {
        assertEquals(0, index.size())
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.add(2L, floatArrayOf(0f, 1f, 0f, 0f))
        assertEquals(2, index.size())
    }

    @Test
    fun clear_emptiesIndex() = runTest {
        index.add(1L, floatArrayOf(1f, 0f, 0f, 0f))
        index.clear()
        assertEquals(0, index.size())
        assertTrue(index.search(floatArrayOf(1f, 0f, 0f, 0f), topK = 1).isEmpty())
    }

    @Test
    fun serialize_roundTrip_preservesSearchResults() = runTest {
        index.add(1L, normalize(floatArrayOf(1f, 0f, 0f, 0f)))
        index.add(2L, normalize(floatArrayOf(0f, 1f, 0f, 0f)))
        index.add(3L, normalize(floatArrayOf(0f, 0f, 1f, 0f)))

        val query = normalize(floatArrayOf(1f, 1f, 0f, 0f))
        val before = index.search(query, topK = 2).map { it.id }

        val bytes = index.serialize()
        val restored = HnswIndex.deserialize(bytes, dimensions = 4)
        val after = restored.search(query, topK = 2).map { it.id }

        assertEquals(before, after)
        assertEquals(3, restored.size())
    }

    @Test
    fun factory_createsHnswIndex() {
        val factoryIndex = VectorIndexFactory.createHnsw(8)
        assertEquals(8, factoryIndex.dimensions())
        assertTrue(factoryIndex is HnswIndex)
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.fold(0f) { acc, v -> acc + v * v })
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }
}
