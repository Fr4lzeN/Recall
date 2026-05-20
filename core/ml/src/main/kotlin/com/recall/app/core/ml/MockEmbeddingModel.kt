package com.recall.app.core.ml

import android.graphics.Bitmap
import kotlin.math.sqrt
import kotlin.random.Random

class MockEmbeddingModel(
    private val profile: ModelProfile = ModelProfile.LITE,
) : EmbeddingModel {

    override val dimensions: Int = profile.dimensions
    override val profileName: String = profile.name

    override suspend fun embedImage(bitmap: Bitmap): FloatArray {
        val seed = bitmap.width * 31 + bitmap.height
        return generateNormalizedVector(seed)
    }

    override suspend fun embedText(text: String): FloatArray {
        val seed = text.hashCode()
        return generateNormalizedVector(seed)
    }

    override fun close() { /* no-op */ }

    private fun generateNormalizedVector(seed: Int): FloatArray {
        val random = Random(seed)
        val vector = FloatArray(dimensions) { random.nextFloat() * 2f - 1f }
        val norm = sqrt(vector.fold(0f) { acc, v -> acc + v * v })
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }
}
