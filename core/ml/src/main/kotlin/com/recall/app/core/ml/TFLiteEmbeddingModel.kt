package com.recall.app.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt
import kotlin.random.Random

class TFLiteEmbeddingModel(
    context: Context,
    private val profile: ModelProfile,
) : EmbeddingModel {

    override val dimensions: Int = profile.dimensions
    override val profileName: String = profile.name

    private val interpreter: Interpreter
    private val delegates: List<AutoCloseable>
    private val inputShape: IntArray
    private val inputLayout: InputLayout

    init {
        val modelBuffer = loadModelFile(context, profile.modelFileName)
        val (options, createdDelegates) = createInterpreterOptions(profile)
        delegates = createdDelegates
        interpreter = Interpreter(modelBuffer, options)
        inputShape = interpreter.getInputTensor(0).shape()
        inputLayout = detectInputLayout(inputShape)
    }

    override suspend fun embedImage(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val preprocessed = ImagePreprocessor.preprocess(bitmap, INPUT_SIZE)
        val pixels = ImagePreprocessor.bitmapToFloatArray(
            preprocessed,
            ImagePreprocessor.Normalization.CLIP_IMAGENET,
        )
        val input = packInput(pixels, preprocessed.width, preprocessed.height)
        runInference(input)
    }

    override suspend fun embedText(text: String): FloatArray {
        val fingerprint = TextFingerprintBitmap.create(text, INPUT_SIZE)
        return try {
            embedImage(fingerprint)
        } finally {
            if (!fingerprint.isRecycled) {
                fingerprint.recycle()
            }
        }
    }

    override fun close() {
        interpreter.close()
        delegates.forEach { delegate ->
            runCatching { delegate.close() }
        }
    }

    private fun runInference(input: Any): FloatArray {
        val output = Array(1) { FloatArray(dimensions) }
        synchronized(interpreter) {
            interpreter.run(input, output)
        }
        return l2Normalize(output[0])
    }

    private fun packInput(pixels: FloatArray, width: Int, height: Int): Any {
        return when (inputLayout) {
            InputLayout.NHWC -> packNhwc(pixels, width, height)
            InputLayout.NCHW -> packNchw(pixels, width, height)
        }
    }

    private fun packNhwc(pixels: FloatArray, width: Int, height: Int): Array<Array<Array<FloatArray>>> {
        val batch = Array(1) {
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
        return batch
    }

    private fun packNchw(pixels: FloatArray, width: Int, height: Int): Array<Array<Array<FloatArray>>> {
        val batch = Array(1) {
            Array(3) { channel ->
                Array(height) { row ->
                    FloatArray(width) { col ->
                        val planeOffset = channel * width * height
                        pixels[planeOffset + row * width + col]
                    }
                }
            }
        }
        return batch
    }

    internal enum class InputLayout { NHWC, NCHW }

    companion object {
        private const val INPUT_SIZE = 224

        fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
            return context.assets.openFd(fileName).use { assetFd ->
                FileInputStream(assetFd.fileDescriptor).use { inputStream ->
                    inputStream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        assetFd.startOffset,
                        assetFd.declaredLength,
                    )
                }
            }
        }

        internal fun createInterpreterOptions(profile: ModelProfile): Pair<Interpreter.Options, List<AutoCloseable>> {
            val options = Interpreter.Options()
            val delegates = mutableListOf<AutoCloseable>()
            val threadCount = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
            options.setNumThreads(threadCount)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching {
                    val nnApiDelegate = NnApiDelegate()
                    options.addDelegate(nnApiDelegate)
                    delegates += nnApiDelegate
                }
            }

            if (profile.quantization != QuantizationType.INT8) {
                runCatching {
                    val gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                    delegates += gpuDelegate
                }
            }

            return options to delegates
        }

        internal fun detectInputLayout(shape: IntArray): InputLayout {
            if (shape.size == 4) {
                when {
                    shape[1] == 3 -> return InputLayout.NCHW
                    shape[3] == 3 -> return InputLayout.NHWC
                    shape[1] == INPUT_SIZE && shape[3] == 3 -> return InputLayout.NHWC
                    shape[1] == 3 && shape[2] == INPUT_SIZE -> return InputLayout.NCHW
                }
            }
            return InputLayout.NHWC
        }

        internal fun l2Normalize(vector: FloatArray): FloatArray {
            var sumSq = 0f
            for (value in vector) {
                sumSq += value * value
            }
            val norm = sqrt(sumSq)
            if (norm <= 0f) return vector
            return FloatArray(vector.size) { i -> vector[i] / norm }
        }
    }
}

internal object TextFingerprintBitmap {
    fun create(text: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val random = Random(text.hashCode())
        val pixels = IntArray(size * size)
        for (i in pixels.indices) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            pixels[i] = Color.rgb(r, g, b)
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
}
