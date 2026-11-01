package com.recall.app.feature.search

import com.recall.app.core.vector.SearchResult

internal data class SearchableMedia(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val dateTaken: Long?,
    val bucketId: String,
    val isDeleted: Boolean,
)

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
            val results = candidates.asSequence()
                .filter { it.score.isFinite() && it.score >= minSimilarity }
                .distinctBy { it.id }
                .mapNotNull { result ->
                    val media = mediaById[result.id] ?: return@mapNotNull null
                    if (media.isDeleted || media.bucketId in excludedBucketIds) {
                        return@mapNotNull null
                    }
                    SearchResultItem(
                        mediaId = media.id,
                        uri = media.uri,
                        displayName = media.displayName,
                        score = result.score,
                        mimeType = media.mimeType,
                        dateTaken = media.dateTaken,
                    )
                }
                .take(limit)
                .toList()
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
}
