package com.recall.app.core.ml

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImagePreprocessorClipTest {
    @Test
    fun preprocess_centerCropsWideImageWithoutStretching() {
        val bitmap = Bitmap.createBitmap(6, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        for (x in 2..3) for (y in 0..1) bitmap.setPixel(x, y, Color.RED)

        val result = ImagePreprocessor.preprocess(bitmap, 2)

        assertEquals(2, result.width)
        assertEquals(2, result.height)
        for (x in 0..1) for (y in 0..1) assertEquals(Color.RED, result.getPixel(x, y))
        assertFalse(bitmap.isRecycled)
        assertEquals(Color.BLUE, bitmap.getPixel(0, 0))
    }

    @Test
    fun preprocess_centerCropsTallImageWithoutStretching() {
        val bitmap = Bitmap.createBitmap(2, 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        for (x in 0..1) for (y in 2..3) bitmap.setPixel(x, y, Color.RED)

        val result = ImagePreprocessor.preprocess(bitmap, 2)

        for (x in 0..1) for (y in 0..1) assertEquals(Color.RED, result.getPixel(x, y))
        assertFalse(bitmap.isRecycled)
    }

    @Test
    fun bitmapToFloatArray_clipImagenet_normalizesChannels() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(255, 128, 0))

        val floats = ImagePreprocessor.bitmapToFloatArray(
            bitmap,
            ImagePreprocessor.Normalization.CLIP_IMAGENET,
        )

        assertEquals((1f - 0.485f) / 0.229f, floats[0], 1e-4f)
        assertEquals((128 / 255f - 0.456f) / 0.224f, floats[1], 1e-4f)
        assertEquals((0f - 0.406f) / 0.225f, floats[2], 1e-4f)
    }
}
