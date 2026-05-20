package com.recall.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.ml.DeviceInfo
import com.recall.app.core.ml.DeviceProfiler
import com.recall.app.core.ml.ModelProfile
import com.recall.app.core.ml.ModelProfileSelector
import com.recall.app.core.vector.VectorIndex
import com.recall.app.core.worker.IndexingPipelineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val vectorIndex: VectorIndex,
) : ViewModel() {
    val totalMedia: StateFlow<Int> = mediaItemDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val indexedMedia: StateFlow<Int> = mediaItemDao.observeIndexedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeProfile: ModelProfile = modelProfileSelector.selectProfile()
    val deviceInfo: DeviceInfo = deviceProfiler.profile()

    val isIndexing: StateFlow<Boolean> = indexingPipelineManager.observePipelineStatus()
        .map { workInfos ->
            workInfos.any { info ->
                info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun reindexAll() {
        indexingPipelineManager.startFullIndexing()
    }

    fun clearIndex() {
        viewModelScope.launch {
            vectorIndex.clear()
            val ids = mediaItemDao.getAllActiveIds()
            for (id in ids) {
                mediaItemDao.markIndexed(
                    id = id,
                    isIndexed = false,
                    version = 0,
                    segmentId = 0,
                    localIndex = 0,
                )
            }
        }
    }
}
