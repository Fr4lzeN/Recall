package com.recall.app.core.ml

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class MockEmbeddingModelTest {
    @Test
    fun dimensions_matchProfile() {
        ModelProfile.ALL.forEach { profile ->
            val model = MockEmbeddingModel(profile)
            assertEquals(profile.dimensions, model.dimensions)
            assertEquals(profile.name, model.profileName)
        }
    }

    @Test
    fun embedText_outputIsL2Normalized() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val embedding = model.embedText("hello recall")
        assertEquals(ModelProfile.LITE.dimensions, embedding.size)
        assertEquals(1.0f, l2Norm(embedding), 1e-5f)
    }

    @Test
    fun embedText_sameInput_isDeterministic() = runTest {
        val model = MockEmbeddingModel(ModelProfile.STANDARD)
        val text = "deterministic test"
        val first = model.embedText(text)
        val second = model.embedText(text)
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun embedText_differentInputs_produceDifferentOutputs() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val a = model.embedText("input a")
        val b = model.embedText("input b")
        assertNotEquals(a.toList(), b.toList())
    }

    private fun l2Norm(vector: FloatArray): Float {
        return sqrt(vector.fold(0f) { acc, v -> acc + v * v })
    }
}
