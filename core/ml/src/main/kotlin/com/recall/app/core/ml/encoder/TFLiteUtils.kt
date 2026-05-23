package com.recall.app.core.ml.encoder

import android.content.Context
import android.os.Build
import com.recall.app.core.ml.ModelProfile
import com.recall.app.core.ml.QuantizationType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

internal object TFLiteUtils {

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSq = 0f
        for (value in vector) {
            sumSq += value * value
        }
        val norm = sqrt(sumSq)
        if (norm <= 0f) return vector
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }

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

    fun createInterpreterOptions(profile: ModelProfile): Pair<Interpreter.Options, List<AutoCloseable>> {
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

    enum class InputLayout { NHWC, NCHW }

    fun detectInputLayout(shape: IntArray): InputLayout {
        if (shape.size == 4) {
            when {
                shape[1] == 3 -> return InputLayout.NCHW
                shape[3] == 3 -> return InputLayout.NHWC
            }
        }
        return InputLayout.NHWC
    }
}
