package com.recall.app.core.ml.encoder

import android.graphics.Bitmap
import com.recall.app.core.ml.ImagePreprocessor
import com.recall.app.core.ml.ModelProfile
import org.tensorflow.lite.Interpreter

class TFLiteImageEncoder(
    private val interpreter: Interpreter,
    private val profile: ModelProfile,
    private val delegates: List<AutoCloseable>,
) {
    private val inputShape: IntArray = interpreter.getInputTensor(0).shape()
    private val inputLayout: TFLiteUtils.InputLayout = TFLiteUtils.detectInputLayout(inputShape)

    fun embed(bitmap: Bitmap): FloatArray {
        val preprocessed = ImagePreprocessor.preprocess(bitmap, profile.inputImageSize)
        val normalization = if (profile.useSimpleNormalization) {
            ImagePreprocessor.Normalization.SIMPLE
        } else {
            ImagePreprocessor.Normalization.CLIP_IMAGENET
        }
        val pixels = ImagePreprocessor.bitmapToFloatArray(preprocessed, normalization)
        val input = packInput(pixels, preprocessed.width, preprocessed.height)
        return runInference(input)
    }

    fun close() {
        interpreter.close()
        delegates.forEach { runCatching { it.close() } }
    }

    private fun runInference(input: Any): FloatArray {
        val output = Array(1) { FloatArray(profile.dimensions) }
        synchronized(interpreter) {
            interpreter.run(input, output)
        }
        return TFLiteUtils.l2Normalize(output[0])
    }

    private fun packInput(pixels: FloatArray, width: Int, height: Int): Any {
        return when (inputLayout) {
            TFLiteUtils.InputLayout.NHWC -> packNhwc(pixels, width, height)
            TFLiteUtils.InputLayout.NCHW -> packNchw(pixels, width, height)
        }
    }

    private fun packNhwc(
        pixels: FloatArray,
        width: Int,
        height: Int,
    ): Array<Array<Array<FloatArray>>> {
        return Array(1) {
            Array(height) { row ->
                Array(width) { col ->
                    val idx = row * width + col
                    floatArrayOf(
                        pixels[idx],
                        pixels[width * height + idx],
                        pixels[2 * width * height + idx],
                    )
                }
            }
        }
    }

    private fun packNchw(
        pixels: FloatArray,
        width: Int,
        height: Int,
    ): Array<Array<Array<FloatArray>>> {
        return Array(1) {
            Array(3) { channel ->
                Array(height) { row ->
                    FloatArray(width) { col ->
                        val planeOffset = channel * width * height
                        pixels[planeOffset + row * width + col]
                    }
                }
            }
        }
    }
}
