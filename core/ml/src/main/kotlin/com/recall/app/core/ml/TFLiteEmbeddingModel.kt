package com.recall.app.core.ml

import android.content.Context
import android.graphics.Bitmap
import com.recall.app.core.ml.encoder.TFLiteImageEncoder
import com.recall.app.core.ml.encoder.TFLiteTextEncoder
import com.recall.app.core.ml.encoder.TFLiteUtils
import com.recall.app.core.ml.tokenizer.ClipBpeTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

class TFLiteEmbeddingModel(
    context: Context,
    private val profile: ModelProfile,
) : EmbeddingModel {

    override val dimensions: Int = profile.dimensions
    override val profileName: String = profile.name

    private val imageEncoder: TFLiteImageEncoder
    private val textEncoder: TFLiteTextEncoder
    private val tokenizer: ClipBpeTokenizer

    init {
        val imgBuffer = TFLiteUtils.loadModelFile(context, profile.imageModelFileName)
        val (imgOptions, imgDelegates) = TFLiteUtils.createInterpreterOptions(profile)
        val imgInterpreter = Interpreter(imgBuffer, imgOptions)
        imageEncoder = TFLiteImageEncoder(imgInterpreter, profile, imgDelegates)

        val txtFileName = requireNotNull(profile.textModelFileName) {
            "ModelProfile must have a textModelFileName for TFLiteEmbeddingModel"
        }
        val txtBuffer = TFLiteUtils.loadModelFile(context, txtFileName)
        val (txtOptions, txtDelegates) = TFLiteUtils.createInterpreterOptions(profile)
        val txtInterpreter = Interpreter(txtBuffer, txtOptions)
        textEncoder = TFLiteTextEncoder(txtInterpreter, profile, txtDelegates)

        tokenizer = ClipBpeTokenizer.create(context, profile.maxTextTokens)
    }

    override suspend fun embedImage(bitmap: Bitmap): FloatArray =
        withContext(Dispatchers.Default) {
            imageEncoder.embed(bitmap)
        }

    override suspend fun embedText(text: String): FloatArray =
        withContext(Dispatchers.Default) {
            val tokens = tokenizer.encode(text)
            textEncoder.embed(tokens)
        }

    override fun close() {
        imageEncoder.close()
        textEncoder.close()
    }

    companion object {
        internal fun l2Normalize(vector: FloatArray): FloatArray =
            TFLiteUtils.l2Normalize(vector)

        internal fun detectInputLayout(shape: IntArray): TFLiteUtils.InputLayout =
            TFLiteUtils.detectInputLayout(shape)
    }
}
