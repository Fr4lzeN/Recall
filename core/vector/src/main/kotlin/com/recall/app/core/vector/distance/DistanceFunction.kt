package com.recall.app.core.vector.distance

enum class DistanceMetric {
    COSINE_SIMILARITY,
    DOT_PRODUCT,
    EUCLIDEAN,
}

object VectorDistance {
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vector dimensions must match: ${a.size} vs ${b.size}" }
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator > 0f) dotProduct / denominator else 0f
    }

    fun dotProduct(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var result = 0f
        for (i in a.indices) result += a[i] * b[i]
        return result
    }

    fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return kotlin.math.sqrt(sum)
    }
}
