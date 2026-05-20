package com.recall.app.feature.search

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val indexedCount: Int = 0,
    val totalCount: Int = 0,
    val error: String? = null,
)

data class SearchResultItem(
    val mediaId: Long,
    val uri: String,
    val displayName: String,
    val score: Float,
    val mimeType: String,
    val dateTaken: Long?,
)
