package com.recall.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.database.dao.IndexingJobDao
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.media.scanner.MediaScanner
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.database.ExcludedDirectoriesRepository
import com.recall.app.core.worker.IndexingPipelineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderUiModel(
    val bucketId: String,
    val name: String,
    val path: String,
    val itemCount: Int,
    val coverUris: List<String>,
    val included: Boolean,
)

data class DirectoryExclusionUiState(
    val folders: List<FolderUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val savedExcludedIds: Set<String> = emptySet(),
) {
    val hasChanges: Boolean
        get() = currentExcludedIds != savedExcludedIds

    val excludedCount: Int
        get() = folders.count { !it.included }

    val skippedItems: Int
        get() = folders.filter { !it.included }.sumOf { it.itemCount }

    private val currentExcludedIds: Set<String>
        get() = folders.filter { !it.included }.map { it.bucketId }.toSet()
}

@HiltViewModel
class DirectoryExclusionViewModel @Inject constructor(
    private val mediaScanner: MediaScanner,
    private val excludedDirectoriesRepository: ExcludedDirectoriesRepository,
    private val indexingPipelineManager: IndexingPipelineManager,
    private val vectorIndex: VectorIndex,
    private val mediaItemDao: MediaItemDao,
    private val indexingJobDao: IndexingJobDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DirectoryExclusionUiState())
    val uiState: StateFlow<DirectoryExclusionUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    fun toggleFolder(bucketId: String) {
        _uiState.update { state ->
            state.copy(
                folders = state.folders.map { folder ->
                    if (folder.bucketId == bucketId) {
                        folder.copy(included = !folder.included)
                    } else {
                        folder
                    }
                },
            )
        }
    }

    fun applyWithoutReindex(onComplete: () -> Unit) {
        viewModelScope.launch {
            persistExclusions(reindex = false)
            onComplete()
        }
    }

    fun applyAndReindex(onComplete: () -> Unit) {
        viewModelScope.launch {
            persistExclusions(reindex = true)
            onComplete()
        }
    }

    private suspend fun persistExclusions(reindex: Boolean) {
        val excludedIds = _uiState.value.folders
            .filter { !it.included }
            .map { it.bucketId }
            .toSet()
        excludedDirectoriesRepository.saveExcludedBucketIds(excludedIds)
        _uiState.update { it.copy(savedExcludedIds = excludedIds) }

        if (excludedIds.isNotEmpty()) {
            val mediaIds = mediaScanner.getMediaIdsInBuckets(excludedIds)
            if (mediaIds.isNotEmpty()) {
                indexingJobDao.deleteByMediaItemIds(mediaIds)
            }
            if (reindex) {
                for (mediaId in mediaIds) {
                    if (vectorIndex.contains(mediaId)) {
                        vectorIndex.remove(mediaId)
                    }
                    mediaItemDao.markIndexed(
                        id = mediaId,
                        isIndexed = false,
                        version = 0,
                        segmentId = 0,
                        localIndex = 0,
                    )
                }
                indexingPipelineManager.startFullReindex()
            }
        } else if (reindex) {
            indexingPipelineManager.startFullReindex()
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
            val folders = mediaScanner.scanFolders().map { folder ->
                FolderUiModel(
                    bucketId = folder.bucketId,
                    name = folder.displayName,
                    path = folder.path,
                    itemCount = folder.itemCount,
                    coverUris = folder.coverUris,
                    included = folder.bucketId !in excludedIds,
                )
            }
            _uiState.update {
                it.copy(
                    folders = folders,
                    savedExcludedIds = excludedIds,
                    isLoading = false,
                )
            }
        }
    }
}
