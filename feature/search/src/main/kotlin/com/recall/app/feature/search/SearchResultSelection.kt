package com.recall.app.feature.search

import com.recall.app.core.vector.SearchResult
import kotlinx.coroutines.CancellationException

internal data class SearchableMedia(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val dateTaken: Long?,
    val bucketId: String,
    val isDeleted: Boolean,
)

internal interface SearchSelectionObserver {
    fun onRound(requested: Int, candidates: List<SearchResult>) = Unit
    fun onCandidate(rank: Int, result: SearchResult, media: SearchableMedia?, disposition: String) = Unit
}

internal object SearchResultSelection {
    // A user-adjustable starting point, not a calibrated relevance probability.
    const val DEFAULT_MIN_SIMILARITY = 0.20f
    const val NO_SIMILARITY_FILTER = -1f
    const val RESULT_LIMIT = 50
    private const val DATABASE_BATCH_SIZE = 800

    suspend fun collect(
        indexSize: Int,
        minSimilarity: Float,
        excludedBucketIds: Set<String>,
        searchCandidates: suspend (Int) -> List<SearchResult>,
        loadMedia: suspend (List<Long>) -> List<SearchableMedia>,
        limit: Int = RESULT_LIMIT,
        observer: SearchSelectionObserver? = null,
    ): List<SearchResultItem> {
        if (indexSize <= 0 || limit <= 0) return emptyList()
        val checkedIds = mutableSetOf<Long>()
        val mediaById = mutableMapOf<Long, SearchableMedia>()
        var requested = minOf(limit, indexSize)

        while (true) {
            val candidates = searchCandidates(requested)
            val uncheckedIds = candidates.asSequence()
                .map { it.id }
                .filter { it >= 0 && checkedIds.add(it) }
                .toList()
            for (ids in uncheckedIds.chunked(DATABASE_BATCH_SIZE)) {
                loadMedia(ids).forEach { mediaById[it.id] = it }
            }
            notifyObserver { observer?.onRound(requested, candidates) }
            val seenIds = mutableSetOf<Long>()
            val results = ArrayList<SearchResultItem>(limit)
            for ((index, result) in candidates.withIndex()) {
                if (observer == null && results.size == limit) break
                val media = mediaById[result.id]
                val disposition = when {
                    !result.score.isFinite() -> "invalid_score"
                    result.score < minSimilarity -> "below_similarity"
                    !seenIds.add(result.id) -> "duplicate"
                    result.id < 0L -> "invalid_id"
                    media == null -> "missing_metadata"
                    media.isDeleted -> "deleted"
                    media.bucketId in excludedBucketIds -> "excluded_directory"
                    results.size == limit -> "beyond_limit"
                    else -> "displayed"
                }
                if (disposition == "displayed" && media != null) {
                    results.add(SearchResultItem(
                        mediaId = media.id,
                        uri = media.uri,
                        displayName = media.displayName,
                        score = result.score,
                        mimeType = media.mimeType,
                        dateTaken = media.dateTaken,
                    ))
                }
                notifyObserver { observer?.onCandidate(index + 1, result, media, disposition) }
            }
            val belowCutoff = candidates.lastOrNull()?.score?.let {
                it.isFinite() && it < minSimilarity
            } ?: false
            if (results.size == limit || requested == indexSize || candidates.size < requested || belowCutoff) {
                return results
            }
            // Excluded, missing and deleted rows must not consume the visible top-K.
            requested = minOf(indexSize.toLong(), requested.toLong() * 2).toInt()
        }
    }

    private inline fun notifyObserver(action: () -> Unit) {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Diagnostic observers must not change the search result if recording fails.
        }
    }
}
