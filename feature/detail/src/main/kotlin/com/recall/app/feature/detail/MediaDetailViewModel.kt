package com.recall.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MediaDetailUiState(
    val mediaId: String = "",
    val isLoading: Boolean = false,
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mediaId: String = checkNotNull(savedStateHandle["mediaId"])

    private val _uiState = MutableStateFlow(MediaDetailUiState(mediaId = mediaId))
    val uiState: StateFlow<MediaDetailUiState> = _uiState.asStateFlow()
}
