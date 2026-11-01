package com.recall.app.core.common.diagnostics

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

internal object SearchTraceJson {
    fun encode(record: SearchDiagnosticRecord): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("traceId", record.traceId)
        put("timestampMillis", record.timestampMillis)
        put("durationMillis", record.durationMillis)
        put("query", record.query.take(8192))
        put("queryCharacterCount", record.query.length)
        put("queryCaptureTruncated", record.query.length > 8192)
        put("status", record.status)
        put("error", record.error ?: JSONObject.NULL)
        put("model", JSONObject().apply {
            put("name", record.modelName)
            put("id", record.modelId)
            put("dimensions", record.dimensions)
        })
        put("index", JSONObject().apply {
            put("type", record.indexType)
            put("sizeAtStart", record.indexSizeAtStart)
            put("sizeAtEnd", record.indexSizeAtEnd)
            put("indexedMediaCount", record.indexedMediaCount)
            put("totalMediaCount", record.totalMediaCount)
        })
        put("minSimilarity", finite(record.minSimilarity))
        put("queryVector", record.queryVector?.let(::vector) ?: JSONObject.NULL)
        put("text", record.text?.let { text -> JSONObject().apply {
            put("normalizedText", text.normalizedText ?: JSONObject.NULL)
            put("tokenIds", text.tokenIds?.let { ids -> JSONArray(ids.toList()) } ?: JSONObject.NULL)
            put("tokenPieces", text.tokenPieces?.let(::JSONArray) ?: JSONObject.NULL)
            put("originalTokenCount", text.originalTokenCount ?: JSONObject.NULL)
            put("truncated", text.truncated ?: JSONObject.NULL)
        } } ?: JSONObject.NULL)
        put("roundCount", record.rounds.size)
        put("rounds", JSONArray(record.rounds.take(32).map { round -> JSONObject().apply {
            put("requested", round.requested)
            put("returned", round.returned)
            put("durationMillis", round.durationMillis)
            put("candidateIds", JSONArray(round.candidateIds.take(SearchDiagnostics.MAX_CANDIDATES)))
            put("candidateScores", JSONArray(round.candidateScores.take(SearchDiagnostics.MAX_CANDIDATES).map(::finite)))
            put("captureTruncated", round.returned > SearchDiagnostics.MAX_CANDIDATES)
        } }))
        put("uniqueCandidateCount", record.uniqueCandidateCount)
        put("candidateLimit", record.candidateLimit)
        put("capturedCandidateCount", minOf(record.candidates.size, SearchDiagnostics.MAX_CANDIDATES))
        val capturedUniqueCount = record.candidates.take(SearchDiagnostics.MAX_CANDIDATES).map { it.mediaId }.distinct().size
        put("capturedUniqueCandidateCount", capturedUniqueCount)
        put("candidateCaptureTruncated", record.uniqueCandidateCount > capturedUniqueCount)
        put("candidates", JSONArray(record.candidates.take(SearchDiagnostics.MAX_CANDIDATES).map(::candidate)))
        put("displayedIds", JSONArray(record.displayedIds))
        put("exactReference", record.exactReference?.let { reference -> JSONObject().apply {
            put("available", reference.available)
            put("reason", reference.reason ?: JSONObject.NULL)
            put("snapshotCount", reference.snapshotCount)
            put("snapshotRevision", reference.snapshotRevision ?: JSONObject.NULL)
            put("isStable", reference.isStable)
            put("searchMethod", reference.searchMethod ?: JSONObject.NULL)
            put("comparedTopK", reference.comparedTopK)
            put("recallAtK", reference.recallAtK?.let(::finite) ?: JSONObject.NULL)
            put("top", JSONArray(reference.top.take(50).map(::candidate)))
        } } ?: JSONObject.NULL)
    }.toString()

    private fun candidate(candidate: DiagnosticCandidate) = JSONObject().apply {
        put("mediaId", candidate.mediaId)
        put("rank", candidate.rank ?: JSONObject.NULL)
        put("score", candidate.score?.let(::finite) ?: JSONObject.NULL)
        put("distance", candidate.distance?.let(::finite) ?: JSONObject.NULL)
        put("disposition", candidate.disposition)
        put("uri", candidate.uri ?: JSONObject.NULL)
        put("displayName", candidate.displayName ?: JSONObject.NULL)
        put("vector", candidate.vector?.let(::vector) ?: JSONObject.NULL)
        put("exactCosine", candidate.exactCosine?.let(::finite) ?: JSONObject.NULL)
        put("scoreDelta", if (candidate.score != null && candidate.exactCosine != null) {
            finite(candidate.score - candidate.exactCosine)
        } else JSONObject.NULL)
    }

    private fun vector(values: FloatArray) = JSONObject().apply {
        val valid = values.filter { it.isFinite() }
        val bytes = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { bytes.putFloat(it) }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes.array())
            .joinToString("") { "%02x".format(it) }
        put("dimensions", values.size)
        put("sha256Float32LittleEndian", hash)
        put("nonFiniteCount", values.size - valid.size)
        put("norm", if (valid.size == values.size) sqrt(valid.sumOf { it.toDouble() * it }) else JSONObject.NULL)
        put("min", valid.minOrNull()?.let(::finite) ?: JSONObject.NULL)
        put("max", valid.maxOrNull()?.let(::finite) ?: JSONObject.NULL)
        put("values", JSONArray(values.map(::finite)))
    }

    private fun finite(value: Float): Any = if (value.isFinite()) value.toDouble() else JSONObject.NULL
}
