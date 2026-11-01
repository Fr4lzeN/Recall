package com.recall.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.common.diagnostics.DiagnosticText
import com.recall.app.core.common.diagnostics.SearchDiagnostics
import com.recall.app.core.database.ExcludedDirectoriesRepository
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.vector.VectorIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val embeddingModel: EmbeddingModel,
    private val vectorIndex: VectorIndex,
    private val mediaItemDao: MediaItemDao,
    private val excludedDirectoriesRepository: ExcludedDirectoriesRepository,
    private val searchDiagnostics: SearchDiagnostics,
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
            var capture: SearchTraceCapture? = null
            try {
                delay(300)
                if (searchDiagnostics.enabled.value) {
                    val state = _uiState.value
                    capture = SearchTraceCapture(
                        query = query,
                        modelName = embeddingModel.modelName,
                        modelId = embeddingModel.modelId,
                        dimensions = embeddingModel.dimensions,
                        indexType = vectorIndex.javaClass.simpleName,
                        indexSizeAtStart = vectorIndex.size(),
                        indexedMediaCount = state.indexedCount,
                        totalMediaCount = state.totalCount,
                        minSimilarity = minSimilarity,
                    )
                }
                embeddingModel.initializationError?.let { error(it) }
                val trace = capture
                val queryEmbedding = if (trace == null) {
                    embeddingModel.embedText(query)
                } else {
                    val result = embeddingModel.embedTextWithDiagnostics(query)
                    trace.embedding(result.vector, DiagnosticText(
                        normalizedText = result.normalizedText,
                        tokenIds = result.tokenIds,
                        tokenPieces = result.tokenPieces,
                        originalTokenCount = result.originalTokenCount,
                        truncated = result.truncated,
                    ))
                    try {
                        trace.snapshot(withContext(Dispatchers.Default) { vectorIndex.diagnosticSnapshot() })
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        trace.snapshot(null, "snapshot_failed")
                    }
                    result.vector
                }
                val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
                val resultItems = SearchResultSelection.collect(
                    indexSize = vectorIndex.size(),
                    minSimilarity = minSimilarity,
                    excludedBucketIds = excludedIds,
                    searchCandidates = { count ->
                        if (trace == null) {
                            vectorIndex.search(queryEmbedding, topK = count)
                        } else {
                            trace.searchCandidates(count) { requested ->
                                withContext(Dispatchers.Default) {
                                    vectorIndex.search(queryEmbedding, topK = requested)
                                }
                            }
                        }
                    },
                    loadMedia = { ids -> loadSearchableMedia(ids, includeDeleted = trace != null) },
                    observer = trace,
                )
                coroutineContext.ensureActive()
                if (revision == searchRevision) {
                    _uiState.update { it.copy(results = resultItems, isSearching = false) }
                }
                saveDiagnostic(trace, "completed", resultItems.map { it.mediaId })
            } catch (e: CancellationException) {
                saveDiagnostic(capture, "cancelled", emptyList())
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
                saveDiagnostic(capture, "failed", emptyList(), e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private suspend fun loadSearchableMedia(ids: List<Long>, includeDeleted: Boolean): List<SearchableMedia> =
        (if (includeDeleted) mediaItemDao.getByIdsIncludingDeleted(ids) else mediaItemDao.getByIds(ids)).map { entity ->
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

    private fun saveDiagnostic(
        capture: SearchTraceCapture?,
        status: String,
        displayedIds: List<Long>,
        error: String? = null,
    ) {
        if (capture == null) return
        val duration = capture.elapsedMillis()
        val endSize = runCatching { vectorIndex.size() }.getOrDefault(-1)
        val endRevision = runCatching { vectorIndex.diagnosticRevision() }.getOrNull()
        searchDiagnostics.enqueue {
            capture.finish(
                status = status,
                error = error,
                displayedIds = displayedIds,
                indexSizeAtEnd = endSize,
                endRevision = endRevision,
                durationMillis = duration,
                loadVector = vectorIndex::getVector,
                loadMedia = { ids -> loadSearchableMedia(ids, includeDeleted = true) },
            )
        }
    }
}
