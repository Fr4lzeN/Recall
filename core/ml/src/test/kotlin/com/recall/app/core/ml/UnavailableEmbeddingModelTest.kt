package com.recall.app.core.ml

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnavailableEmbeddingModelTest {
    @Test
    fun unavailableModelReportsFailureAndNeverCreatesFakeTextEmbedding() = runTest {
        val model = UnavailableEmbeddingModel(ModelProfile.STANDARD, IllegalStateException("Missing vocabulary"))
        assertEquals(512, model.dimensions)
        assertTrue(model.initializationError.contains("Missing vocabulary"))
        try {
            model.embedText("кот")
            throw AssertionError("Unavailable real model must not return an embedding")
        } catch (expected: IllegalStateException) {
            assertEquals(model.initializationError, expected.message)
        }
    }

    @Test
    fun mockAndUnavailableModelsHaveDistinctIdentities() {
        val unavailable = UnavailableEmbeddingModel(ModelProfile.LITE, IllegalStateException("Missing model"))
        val mock = MockEmbeddingModel(ModelProfile.LITE)
        assertNotEquals(unavailable.modelId, mock.modelId)
    }
}
