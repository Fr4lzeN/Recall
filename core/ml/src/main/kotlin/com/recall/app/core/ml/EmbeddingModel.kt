package com.recall.app.core.ml

import android.graphics.Bitmap

interface EmbeddingModel {
    suspend fun embedImage(bitmap: Bitmap): FloatArray
    suspend fun embedText(text: String): FloatArray
    val dimensions: Int
    val profileName: String
    val modelName: String get() = profileName
    val modelId: String get() = "$profileName:$dimensions"
    val initializationError: String? get() = null
    fun close()
}
