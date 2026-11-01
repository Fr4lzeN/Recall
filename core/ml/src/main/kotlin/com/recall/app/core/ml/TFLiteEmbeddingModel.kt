package com.recall.app.core.ml

import android.content.Context
import android.graphics.Bitmap
import com.recall.app.core.ml.encoder.TFLiteImageEncoder
import com.recall.app.core.ml.encoder.TFLiteTextEncoder
import com.recall.app.core.ml.encoder.TFLiteUtils
import com.recall.app.core.ml.tokenizer.ClipBpeTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class TFLiteEmbeddingModel(
    context: Context,
    private val profile: ModelProfile,
) : EmbeddingModel {

    override val dimensions: Int = profile.dimensions
    override val profileName: String = profile.name
    override val modelName: String = profile.modelName
    override val modelId: String

    private val imageEncoder: TFLiteImageEncoder
    private val textEncoder: TFLiteTextEncoder
    private val tokenizer: ClipBpeTokenizer

    init {
        val txtFileName = requireNotNull(profile.textModelFileName) {
            "ModelProfile must have a textModelFileName for TFLiteEmbeddingModel"
        }
        tokenizer = ClipBpeTokenizer.create(context, profile.maxTextTokens)
        val imgBuffer = TFLiteUtils.loadModelFile(context, profile.imageModelFileName)
        val txtBuffer = TFLiteUtils.loadModelFile(context, txtFileName)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("mobileclip-s0:resize-center-crop-v2:clip-bpe-v2:${profile.dimensions}:".toByteArray())
        digest.update("${profile.inputImageSize}:${profile.maxTextTokens}:${profile.useSimpleNormalization}".toByteArray())
        digest.update(imgBuffer.asReadOnlyBuffer())
        digest.update(txtBuffer.asReadOnlyBuffer())
        ClipBpeTokenizer.openVocabulary(context).use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        modelId = "mobileclip-s0:" + digest.digest().joinToString("") { "%02x".format(it) }

        val resources = mutableListOf<AutoCloseable>()
        try {
            val (imgInterpreter, imgDelegates) = TFLiteUtils.openInterpreter(imgBuffer, profile)
            resources.addAll(imgDelegates)
            resources.add(imgInterpreter)
            imageEncoder = TFLiteImageEncoder(imgInterpreter, profile, imgDelegates)
            val (txtInterpreter, txtDelegates) = TFLiteUtils.openInterpreter(txtBuffer, profile)
            resources.addAll(txtDelegates)
            resources.add(txtInterpreter)
            textEncoder = TFLiteTextEncoder(txtInterpreter, profile, txtDelegates)
        } catch (error: Throwable) {
            resources.asReversed().forEach { runCatching { it.close() } }
            throw error
        }
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

    override suspend fun embedTextWithDiagnostics(text: String): TextEmbeddingDiagnosticResult =
        withContext(Dispatchers.Default) {
            val tokens = tokenizer.encodeWithDiagnostics(text)
            TextEmbeddingDiagnosticResult(
                vector = textEncoder.embed(tokens.tokenIds),
                normalizedText = tokens.normalizedText,
                tokenIds = tokens.tokenIds,
                tokenPieces = tokens.tokenPieces,
                originalTokenCount = tokens.originalTokenCount,
                truncated = tokens.truncated,
            )
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
