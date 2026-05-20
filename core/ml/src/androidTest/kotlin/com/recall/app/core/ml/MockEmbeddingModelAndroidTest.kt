package com.recall.app.core.ml

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class MockEmbeddingModelAndroidTest {
    @Test
    fun embedImage_outputIsL2Normalized() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)

        val embedding = model.embedImage(bitmap)
        assertEquals(ModelProfile.LITE.dimensions, embedding.size)
        assertEquals(1.0f, l2Norm(embedding), 1e-5f)
    }

    @Test
    fun embedImage_sameDimensions_isDeterministic() = runTest {
        val model = MockEmbeddingModel(ModelProfile.PRO)
        val bitmapA = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val bitmapB = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)

        assertTrue(model.embedImage(bitmapA).contentEquals(model.embedImage(bitmapB)))
    }

    @Test
    fun embedImage_differentDimensions_produceDifferentOutputs() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val small = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val large = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)

        assertNotEquals(
            model.embedImage(small).toList(),
            model.embedImage(large).toList(),
        )
    }

    private fun l2Norm(vector: FloatArray): Float {
        return sqrt(vector.fold(0f) { acc, v -> acc + v * v })
    }
}
