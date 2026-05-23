package com.recall.app.feature.albums

data class Album(
    val index: Int,
    val name: String,
    val coverUri: String,
    val photoCount: Int,
    val mediaIds: List<Long>,
)

data class AlbumsUiState(
    val isLoading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val error: String? = null,
)
