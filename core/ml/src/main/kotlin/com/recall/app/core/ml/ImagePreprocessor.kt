package com.recall.app.core.ml

import android.graphics.Bitmap

object ImagePreprocessor {
    fun preprocess(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
    }

    fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatArray = FloatArray(3 * width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            floatArray[i] = ((pixel shr 16) and 0xFF) / 255f
            floatArray[width * height + i] = ((pixel shr 8) and 0xFF) / 255f
            floatArray[2 * width * height + i] = (pixel and 0xFF) / 255f
        }
        return floatArray
    }
}
