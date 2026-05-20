package com.recall.app.core.ml

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePreprocessorAndroidTest {
    @Test
    fun preprocess_scalesBitmapToTargetSize() {
        val source = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val scaled = ImagePreprocessor.preprocess(source, targetSize = 224)

        assertEquals(224, scaled.width)
        assertEquals(224, scaled.height)
    }

    @Test
    fun bitmapToFloatArray_extractsNormalizedRgbChannels() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(255, 128, 0))

        val floats = ImagePreprocessor.bitmapToFloatArray(bitmap)

        assertEquals(3 * 2 * 2, floats.size)
        assertEquals(1.0f, floats[0], 1e-5f)
        assertEquals(128 / 255f, floats[4], 1e-5f)
        assertEquals(0.0f, floats[8], 1e-5f)
    }
}
