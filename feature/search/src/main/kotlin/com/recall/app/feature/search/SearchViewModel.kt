package com.recall.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.database.ExcludedDirectoriesRepository
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
    private val mediaItemDao: MediaItemDao,
    private val excludedDirectoriesRepository: ExcludedDirectoriesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                mediaItemDao.observeIndexedCount(),
                mediaItemDao.observeCount(),
            ) { indexed, total -> indexed to total }
                .collect { (indexed, total) ->
                    _uiState.update { it.copy(indexedCount = indexed, totalCount = total) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        search(query)
    }

    fun retrySearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            search(query)
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                delay(300)
                val queryEmbedding = embeddingModel.embedText(query)
                val results = vectorIndex.search(queryEmbedding, topK = 50)
                val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
                val resultItems = results.mapNotNull { searchResult ->
                    mediaItemDao.getById(searchResult.id)?.let { entity ->
                        if (entity.bucketId.isNotEmpty() && entity.bucketId in excludedIds) {
                            return@mapNotNull null
                        }
                        SearchResultItem(
                            mediaId = entity.id,
                            uri = entity.uri,
                            displayName = entity.displayName,
                            score = searchResult.score,
                            mimeType = entity.mimeType,
                            dateTaken = entity.dateTaken,
                        )
                    }
                }
                _uiState.update { it.copy(results = resultItems, isSearching = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Search failed",
                        isSearching = false,
                    )
                }
            }
        }
    }
}
