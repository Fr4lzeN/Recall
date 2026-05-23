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
        assertEquals(1.0f, l2Norm(embedding), 1e-4f)
    }

    @Test
    fun embedImage_samePixels_isDeterministic() = runTest {
        val model = MockEmbeddingModel(ModelProfile.PRO)
        val a = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.BLUE) }
        val b = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.BLUE) }

        assertTrue(model.embedImage(a).contentEquals(model.embedImage(b)))
    }

    @Test
    fun embedImage_differentColors_produceDifferentEmbeddings() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val red = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.RED) }
        val blue = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.BLUE) }

        assertNotEquals(
            model.embedImage(red).toList(),
            model.embedImage(blue).toList(),
        )
    }

    @Test
    fun embedImage_warmColors_closerToSunsetQuery() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val warm = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.rgb(230, 120, 40)) }
        val cool = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.rgb(40, 100, 200)) }

        val sunsetVec = model.embedText("sunset")
        val warmVec = model.embedImage(warm)
        val coolVec = model.embedImage(cool)

        val warmSim = cosine(warmVec, sunsetVec)
        val coolSim = cosine(coolVec, sunsetVec)
        assertTrue("warm image should be closer to 'sunset' than cool image", warmSim > coolSim)
    }

    private fun l2Norm(vector: FloatArray): Float =
        sqrt(vector.fold(0f) { acc, v -> acc + v * v })

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) dot += a[i] * b[i]
        return dot / (l2Norm(a) * l2Norm(b))
    }
}
