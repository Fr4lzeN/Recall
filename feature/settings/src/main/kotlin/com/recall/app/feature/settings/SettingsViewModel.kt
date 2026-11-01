package com.recall.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.ml.DeviceInfo
import com.recall.app.core.ml.DeviceProfiler
import com.recall.app.core.ml.EmbeddingModel
import com.recall.app.core.ml.ModelProfile
import com.recall.app.core.ml.ModelProfileSelector
import com.recall.app.core.media.scanner.MediaScanner
import com.recall.app.core.database.ExcludedDirectoriesRepository
import com.recall.app.core.worker.IndexingPipelineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val modelProfileSelector: ModelProfileSelector,
    private val deviceProfiler: DeviceProfiler,
    private val indexingPipelineManager: IndexingPipelineManager,
    private val excludedDirectoriesRepository: ExcludedDirectoriesRepository,
    private val mediaScanner: MediaScanner,
    embeddingModel: EmbeddingModel,
) : ViewModel() {
    val totalMedia: StateFlow<Int> = mediaItemDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val indexedMedia: StateFlow<Int> = mediaItemDao.observeIndexedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeProfile: ModelProfile = modelProfileSelector.selectProfile()
    val modelProfileName: String = embeddingModel.profileName
    val modelInitializationError: String? = embeddingModel.initializationError
    val modelStatus: String = when {
        modelInitializationError != null -> "Unavailable: $modelInitializationError"
        embeddingModel.modelId.startsWith("mock") -> "Demo model · semantic search is simulated"
        else -> "${activeProfile.modelName} · ${embeddingModel.dimensions} dimensions"
    }
    val deviceInfo: DeviceInfo = deviceProfiler.profile()

    val isIndexing: StateFlow<Boolean> = indexingPipelineManager.observePipelineStatus()
        .map { workInfos ->
            workInfos.any { info ->
                info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val indexingError: StateFlow<String?> = indexingPipelineManager.observePipelineStatus()
        .map { workInfos ->
            val failure = workInfos.firstOrNull { it.state == WorkInfo.State.FAILED }
                ?.outputData?.getString("error")
            val failedPhotos = workInfos.filter { it.state == WorkInfo.State.SUCCEEDED }
                .sumOf { it.outputData.getInt("failed", 0) }
            failure ?: if (failedPhotos > 0) {
                "$failedPhotos photos could not be indexed. Try indexing again."
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _exclusionStats = MutableStateFlow(ExclusionStats())
    val exclusionStats: StateFlow<ExclusionStats> = _exclusionStats.asStateFlow()

    init {
        refreshExclusionStats()
    }

    fun refreshExclusionStats() {
        viewModelScope.launch {
            val excludedIds = excludedDirectoriesRepository.getExcludedBucketIds()
            if (excludedIds.isEmpty()) {
                _exclusionStats.value = ExclusionStats()
                return@launch
            }
            val folders = mediaScanner.scanFolders()
            val excludedFolders = folders.filter { it.bucketId in excludedIds }
            _exclusionStats.value = ExclusionStats(
                excludedFolderCount = excludedFolders.size,
                skippedItemCount = excludedFolders.sumOf { it.itemCount },
            )
        }
    }

    fun reindexAll() {
        viewModelScope.launch {
            indexingPipelineManager.startFullReindex()
        }
    }

    fun clearIndex() {
        viewModelScope.launch {
            indexingPipelineManager.clearIndex()
        }
    }
}

data class ExclusionStats(
    val excludedFolderCount: Int = 0,
    val skippedItemCount: Int = 0,
)
