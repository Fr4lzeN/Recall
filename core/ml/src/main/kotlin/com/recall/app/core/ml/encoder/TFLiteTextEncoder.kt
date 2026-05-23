package com.recall.app.core.ml.encoder

import com.recall.app.core.ml.ModelProfile
import org.tensorflow.lite.Interpreter

class TFLiteTextEncoder(
    private val interpreter: Interpreter,
    private val profile: ModelProfile,
    private val delegates: List<AutoCloseable>,
) {
    private val inputCount: Int = interpreter.inputTensorCount
    private val inputShape: IntArray = interpreter.getInputTensor(0).shape()

    fun embed(tokens: IntArray): FloatArray {
        val output = Array(1) { FloatArray(profile.dimensions) }
        val inputIds = arrayOf(tokens)

        synchronized(interpreter) {
            if (inputCount > 1) {
                val attentionMask = IntArray(tokens.size) { if (tokens[it] != 0) 1 else 0 }
                val inputs = arrayOf(tokens, attentionMask)
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
}
