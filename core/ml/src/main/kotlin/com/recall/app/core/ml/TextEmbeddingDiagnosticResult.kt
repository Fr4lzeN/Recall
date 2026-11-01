package com.recall.app.core.ml

data class TextEmbeddingDiagnosticResult(
    val vector: FloatArray,
    val normalizedText: String? = null,
    val tokenIds: IntArray? = null,
    val tokenPieces: List<String>? = null,
    val originalTokenCount: Int? = null,
    val truncated: Boolean? = null,
)
