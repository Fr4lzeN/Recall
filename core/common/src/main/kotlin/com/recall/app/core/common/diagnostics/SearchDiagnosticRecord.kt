package com.recall.app.core.common.diagnostics

import java.util.UUID

data class DiagnosticText(
    val normalizedText: String? = null,
    val tokenIds: IntArray? = null,
    val tokenPieces: List<String>? = null,
    val originalTokenCount: Int? = null,
    val truncated: Boolean? = null,
)

data class DiagnosticCandidate(
    val mediaId: Long,
    val rank: Int?, // One-based rank in the unfiltered candidates.
    val score: Float?,
    val distance: Float? = null,
    val disposition: String,
    val uri: String? = null,
    val displayName: String? = null,
    val vector: FloatArray? = null,
    val exactCosine: Float? = null,
)

data class DiagnosticSearchRound(
    val requested: Int,
    val returned: Int,
    val durationMillis: Long,
    val candidateIds: List<Long> = emptyList(),
    val candidateScores: List<Float> = emptyList(),
)

data class DiagnosticExactReference(
    val available: Boolean,
    val reason: String? = null,
    val snapshotCount: Int = 0,
    val snapshotRevision: Long? = null,
    val isStable: Boolean = false,
    val searchMethod: String? = null,
    val comparedTopK: Int = 0,
    val recallAtK: Float? = null,
    val top: List<DiagnosticCandidate> = emptyList(),
)

data class SearchDiagnosticRecord(
    val timestampMillis: Long,
    val durationMillis: Long,
    val query: String,
    val modelName: String,
    val modelId: String,
    val dimensions: Int,
    val indexType: String,
    val indexSizeAtStart: Int,
    val indexSizeAtEnd: Int,
    val indexedMediaCount: Int,
    val totalMediaCount: Int,
    val minSimilarity: Float,
    val status: String,
    val queryVector: FloatArray? = null,
    val text: DiagnosticText? = null,
    val rounds: List<DiagnosticSearchRound> = emptyList(),
    val candidates: List<DiagnosticCandidate> = emptyList(),
    val displayedIds: List<Long> = emptyList(),
    val error: String? = null,
    val uniqueCandidateCount: Int = candidates.size,
    val candidateLimit: Int = SearchDiagnostics.MAX_CANDIDATES,
    val exactReference: DiagnosticExactReference? = null,
    val traceId: String = UUID.randomUUID().toString(),
)
