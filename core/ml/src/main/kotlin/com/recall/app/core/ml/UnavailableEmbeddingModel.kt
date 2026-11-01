package com.recall.app.core.ml

import android.graphics.Bitmap

class UnavailableEmbeddingModel(
    profile: ModelProfile,
    cause: Exception,
) : EmbeddingModel {
    override val dimensions: Int = profile.dimensions
    override val profileName: String = profile.name
    override val modelName: String = profile.modelName
    override val modelId: String = "unavailable:${profile.imageModelFileName}"
    override val initializationError: String =
        "Search model could not be loaded. Install a build containing the model and tokenizer. " +
            (cause.message ?: cause.javaClass.simpleName)

    override suspend fun embedImage(bitmap: Bitmap): FloatArray = unavailable()
    override suspend fun embedText(text: String): FloatArray = unavailable()
    override fun close() = Unit

    private fun unavailable(): Nothing = throw IllegalStateException(initializationError)
}
