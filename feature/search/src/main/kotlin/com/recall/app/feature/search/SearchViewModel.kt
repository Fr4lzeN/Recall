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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
    private val mediaItemDao: MediaItemDao,
    private val excludedDirectoriesRepository: ExcludedDirectoriesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(error = embeddingModel.initializationError))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var searchRevision = 0L

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
        searchJob?.cancel()
        searchRevision++
        _uiState.update { it.copy(query = query, error = embeddingModel.initializationError) }
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        search(query)
    }

    fun onMinSimilarityChange(value: Float) {
        val threshold = if (value.isFinite()) value.coerceIn(-1f, 1f) else return
        _uiState.update { it.copy(minSimilarity = threshold) }
        retrySearch()
    }

    fun showNearestPhotos() = onMinSimilarityChange(SearchResultSelection.NO_SIMILARITY_FILTER)

    fun retrySearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            search(query)
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        val revision = ++searchRevision
        val minSimilarity = _uiState.value.minSimilarity
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                delay(300)
                embeddingModel.initializationError?.let { error(it) }
                val queryEmbedding = embeddingModel.embedText(query)
                val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
                val resultItems = SearchResultSelection.collect(
                    indexSize = vectorIndex.size(),
                    minSimilarity = minSimilarity,
                    excludedBucketIds = excludedIds,
                    searchCandidates = { count -> vectorIndex.search(queryEmbedding, topK = count) },
                    loadMedia = { ids ->
                        mediaItemDao.getByIds(ids).map { entity ->
                            SearchableMedia(
                                id = entity.id,
                                uri = entity.uri,
                                displayName = entity.displayName,
                                mimeType = entity.mimeType,
                                dateTaken = entity.dateTaken,
                                bucketId = entity.bucketId,
                                isDeleted = entity.isDeleted,
                            )
                        }
                    },
                )
                coroutineContext.ensureActive()
                if (revision == searchRevision) {
                    _uiState.update { it.copy(results = resultItems, isSearching = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                if (revision == searchRevision) {
                    _uiState.update {
                        it.copy(
                            results = emptyList(),
                            error = e.message ?: "Search failed",
                            isSearching = false,
                        )
                    }
                }
            }
        }
    }
}
