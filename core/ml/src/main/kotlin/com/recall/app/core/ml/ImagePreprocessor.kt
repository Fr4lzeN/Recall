package com.recall.app.core.ml

import android.graphics.Bitmap

object ImagePreprocessor {
    enum class Normalization {
        /** RGB channels scaled to [0, 1]. */
        SIMPLE,
        /** CLIP / ImageNet: (channel/255 - mean) / std. */
        CLIP_IMAGENET,
    }

    private val clipMean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val clipStd = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun preprocess(bitmap: Bitmap, targetSize: Int = 224): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
    }

    fun bitmapToFloatArray(
        bitmap: Bitmap,
        normalization: Normalization = Normalization.SIMPLE,
    ): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatArray = FloatArray(3 * width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            when (normalization) {
                Normalization.SIMPLE -> {
                    floatArray[i] = r
                    floatArray[width * height + i] = g
                    floatArray[2 * width * height + i] = b
                }
                Normalization.CLIP_IMAGENET -> {
                    floatArray[i] = (r - clipMean[0]) / clipStd[0]
                    floatArray[width * height + i] = (g - clipMean[1]) / clipStd[1]
                    floatArray[2 * width * height + i] = (b - clipMean[2]) / clipStd[2]
                }
            }
        }
        return floatArray
    }
}
