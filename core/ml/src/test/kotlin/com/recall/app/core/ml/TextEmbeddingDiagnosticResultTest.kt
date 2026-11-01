package com.recall.app.core.ml

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextEmbeddingDiagnosticResultTest {
    @Test
    fun defaultDiagnosticApiCallsTheActualEmbeddingOnceAndDoesNotInventTokenData() = runBlocking {
        var calls = 0
        val model = object : EmbeddingModel {
            override val dimensions = 3
            override val profileName = "test"
            override suspend fun embedImage(bitmap: Bitmap): FloatArray = error("Unexpected image call")
            override suspend fun embedText(text: String): FloatArray {
                calls++
                assertEquals("кот", text)
                return floatArrayOf(1f, 2f, 3f)
            }
            override fun close() = Unit
        }
        val diagnostic = model.embedTextWithDiagnostics("кот")
        assertEquals(1, calls)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), diagnostic.vector, 0f)
        assertNull(diagnostic.normalizedText)
        assertNull(diagnostic.tokenIds)
        assertNull(diagnostic.tokenPieces)
        assertNull(diagnostic.originalTokenCount)
        assertNull(diagnostic.truncated)
    }
}
