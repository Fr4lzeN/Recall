package com.recall.app.core.ml.encoder

import com.recall.app.core.ml.ModelProfile
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import com.recall.app.core.ml.tokenizer.ClipBpeTokenizer

class TFLiteTextEncoder(
    private val interpreter: Interpreter,
    private val profile: ModelProfile,
    private val delegates: List<AutoCloseable>,
) {
    private val inputCount: Int = interpreter.inputTensorCount
    private val inputShape: IntArray = interpreter.getInputTensor(0).shape()

    fun embed(tokens: IntArray): FloatArray {
        require(inputShape.contentEquals(intArrayOf(1, tokens.size))) { "Unexpected text token shape" }
        require(inputCount in 1..2) { "Expected token IDs and an optional attention mask" }
        val output = Array(1) { FloatArray(profile.dimensions) }
        val inputIds = packTokens(tokens, interpreter.getInputTensor(0).dataType())

        synchronized(interpreter) {
            if (inputCount > 1) {
                val attentionMask = attentionMask(tokens)
                val inputs = arrayOf(inputIds, packTokens(attentionMask, interpreter.getInputTensor(1).dataType()))
                val outputs = mutableMapOf<Int, Any>(0 to output)
                interpreter.runForMultipleInputsOutputs(inputs, outputs)
            } else {
                interpreter.run(inputIds, output)
            }
        }

        return TFLiteUtils.l2Normalize(output[0])
    }

    fun close() {
        interpreter.close()
        delegates.forEach { runCatching { it.close() } }
    }

    companion object {
        internal fun packTokens(tokens: IntArray, dataType: DataType): Any = when (dataType) {
            DataType.INT32 -> arrayOf(tokens)
            DataType.INT64 -> arrayOf(LongArray(tokens.size) { tokens[it].toLong() })
            else -> error("Unsupported text input type: $dataType")
        }

        internal fun attentionMask(tokens: IntArray): IntArray {
            val endIndex = tokens.indexOf(ClipBpeTokenizer.EOT_TOKEN)
            require(endIndex >= 0) { "Text tokens must contain the CLIP end token" }
            return IntArray(tokens.size) { if (it <= endIndex) 1 else 0 }
        }
    }
}
