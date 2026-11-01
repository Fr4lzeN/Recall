package com.recall.app.feature.search

import com.recall.app.core.common.diagnostics.DiagnosticCandidate
import com.recall.app.core.common.diagnostics.DiagnosticExactReference
import com.recall.app.core.common.diagnostics.DiagnosticSearchRound
import com.recall.app.core.common.diagnostics.DiagnosticText
import com.recall.app.core.common.diagnostics.SearchDiagnosticRecord
import com.recall.app.core.common.diagnostics.SearchDiagnostics
import com.recall.app.core.vector.SearchResult
import com.recall.app.core.vector.VectorIndexDiagnosticSnapshot
import com.recall.app.core.vector.distance.VectorDistance
import com.recall.app.core.vector.linear.LinearScanIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** One request owns this capture; it is detached from UI state before asynchronous persistence. */
internal class SearchTraceCapture(
    private val query: String,
    private val modelName: String,
    private val modelId: String,
    private val dimensions: Int,
    private val indexType: String,
    private val indexSizeAtStart: Int,
    private val indexedMediaCount: Int,
    private val totalMediaCount: Int,
    private val minSimilarity: Float,
    private val candidateLimit: Int = SearchDiagnostics.MAX_CANDIDATES,
) : SearchSelectionObserver {
    private data class ObservedCandidate(
        val rank: Int,
        val result: SearchResult,
        val media: SearchableMedia?,
        val disposition: String,
    )

    private val timestampMillis = System.currentTimeMillis()
    private val startedNanos = System.nanoTime()
    private val rounds = mutableListOf<DiagnosticSearchRound>()
    private val observed = mutableListOf<ObservedCandidate>()
    private var firstRawTop = emptyList<SearchResult>()
    private var latestRoundDurationMillis = 0L
    private var uniqueCandidateCount = 0
    private var snapshot: VectorIndexDiagnosticSnapshot? = null
    private var snapshotReason: String? = "snapshot_not_captured"
    private var queryVector: FloatArray? = null
    private var text: DiagnosticText? = null

    init {
        require(candidateLimit >= SearchResultSelection.RESULT_LIMIT)
    }

    fun embedding(vector: FloatArray, diagnosticText: DiagnosticText) {
        queryVector = vector.copyOf()
        text = diagnosticText.copy(
            tokenIds = diagnosticText.tokenIds?.copyOf(),
            tokenPieces = diagnosticText.tokenPieces?.toList(),
        )
    }

    fun snapshot(value: VectorIndexDiagnosticSnapshot?, unavailableReason: String? = null) {
        snapshot = value
        snapshotReason = if (value == null) unavailableReason ?: "snapshot_unavailable_or_limit_exceeded" else null
    }

    suspend fun searchCandidates(
        requested: Int,
        search: suspend (Int) -> List<SearchResult>,
    ): List<SearchResult> {
        val started = System.nanoTime()
        return search(requested).also {
            latestRoundDurationMillis = (System.nanoTime() - started) / 1_000_000
        }
    }

    override fun onRound(requested: Int, candidates: List<SearchResult>) {
        if (rounds.isEmpty()) firstRawTop = candidates.take(SearchResultSelection.RESULT_LIMIT)
        rounds += DiagnosticSearchRound(
            requested = requested,
            returned = candidates.size,
            durationMillis = latestRoundDurationMillis,
            candidateIds = candidates.take(candidateLimit).map { it.id },
            candidateScores = candidates.take(candidateLimit).map { it.score },
        )
        uniqueCandidateCount = candidates.asSequence().map { it.id }.distinct().count()
        observed.clear()
    }

    override fun onCandidate(rank: Int, result: SearchResult, media: SearchableMedia?, disposition: String) {
        val candidate = ObservedCandidate(rank, result, media, disposition)
        if (observed.size < candidateLimit) {
            observed += candidate
        } else if (disposition == "displayed") {
            // Retain every displayed image even if excluded folders pushed its raw rank past the cap.
            val replaceIndex = observed.indexOfLast { it.disposition != "displayed" }
            if (replaceIndex >= 0) observed[replaceIndex] = candidate
        }
    }

    suspend fun finish(
        status: String,
        error: String?,
        displayedIds: List<Long>,
        indexSizeAtEnd: Int,
        endRevision: Long?,
        durationMillis: Long = elapsedMillis(),
        loadVector: suspend (Long) -> FloatArray?,
        loadMedia: suspend (List<Long>) -> List<SearchableMedia>,
    ): SearchDiagnosticRecord {
        val referenceSnapshot = snapshot
        val vectors = referenceSnapshot?.entries?.toMap()?.toMutableMap() ?: mutableMapOf()
        val candidates = observed.sortedBy { it.rank }.map { item ->
            currentCoroutineContext().ensureActive()
            val vector = vectors[item.result.id] ?: safely { loadVector(item.result.id) }
                ?.also { vectors[item.result.id] = it }
            val disposition = if (item.disposition == "displayed" && status != "completed") {
                "selected_before_$status"
            } else item.disposition
            diagnosticCandidate(item.result, item.rank, disposition, item.media, vector)
        }
        val exactReference = safely { exactReference(referenceSnapshot, endRevision, vectors, loadMedia) }
            ?: DiagnosticExactReference(available = false, reason = "reference_comparison_failed")
        return SearchDiagnosticRecord(
            timestampMillis = timestampMillis,
            durationMillis = durationMillis,
            query = query,
            modelName = modelName,
            modelId = modelId,
            dimensions = dimensions,
            indexType = indexType,
            indexSizeAtStart = indexSizeAtStart,
            indexSizeAtEnd = indexSizeAtEnd,
            indexedMediaCount = indexedMediaCount,
            totalMediaCount = totalMediaCount,
            minSimilarity = minSimilarity,
            status = status,
            queryVector = queryVector,
            text = text,
            rounds = rounds.toList(),
            candidates = candidates,
            displayedIds = displayedIds,
            error = error,
            uniqueCandidateCount = uniqueCandidateCount,
            candidateLimit = candidateLimit,
            exactReference = exactReference,
        )
    }

    fun elapsedMillis(): Long = (System.nanoTime() - startedNanos) / 1_000_000

    private suspend fun exactReference(
        referenceSnapshot: VectorIndexDiagnosticSnapshot?,
        endRevision: Long?,
        vectors: Map<Long, FloatArray>,
        loadMedia: suspend (List<Long>) -> List<SearchableMedia>,
    ): DiagnosticExactReference {
        val query = queryVector
        if (referenceSnapshot == null || query == null) {
            return DiagnosticExactReference(
                available = false,
                reason = if (query == null) "query_embedding_unavailable" else snapshotReason,
            )
        }
        if (query.size != dimensions || query.any { !it.isFinite() }) {
            return DiagnosticExactReference(available = false, reason = "invalid_query_embedding")
        }
        val exact = LinearScanIndex(dimensions)
        exact.addBatch(referenceSnapshot.entries)
        val topK = minOf(SearchResultSelection.RESULT_LIMIT, referenceSnapshot.entries.size)
        val top = exact.search(query, topK)
        val metadata = safely { loadMedia(top.map { it.id }) }?.associateBy { it.id }.orEmpty()
        val stable = referenceSnapshot.revision == endRevision
        val recall = if (stable && topK > 0 && rounds.isNotEmpty()) {
            val expectedIds = top.mapTo(mutableSetOf()) { it.id }
            firstRawTop.map { it.id }.distinct().count { it in expectedIds }.toFloat() / expectedIds.size
        } else null
        return DiagnosticExactReference(
            available = true,
            reason = if (stable) null else "index_changed_during_search",
            snapshotCount = referenceSnapshot.entries.size,
            snapshotRevision = referenceSnapshot.revision,
            isStable = stable,
            searchMethod = referenceSnapshot.searchMethod,
            comparedTopK = if (rounds.isEmpty()) 0 else topK,
            recallAtK = recall,
            top = top.mapIndexed { index, result ->
                diagnosticCandidate(result, index + 1, "exact_reference", metadata[result.id], vectors[result.id])
            },
        )
    }

    private fun diagnosticCandidate(
        result: SearchResult,
        rank: Int,
        disposition: String,
        media: SearchableMedia?,
        vector: FloatArray?,
    ) = DiagnosticCandidate(
        mediaId = result.id,
        rank = rank,
        score = result.score,
        distance = result.distance,
        disposition = disposition,
        uri = media?.uri,
        displayName = media?.displayName,
        vector = vector?.copyOf(),
        exactCosine = queryVector?.let { query ->
            vector?.takeIf { it.size == query.size }?.let { VectorDistance.cosineSimilarity(query, it) }
        },
    )

    private suspend fun <T> safely(action: suspend () -> T): T? = try {
        action()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
