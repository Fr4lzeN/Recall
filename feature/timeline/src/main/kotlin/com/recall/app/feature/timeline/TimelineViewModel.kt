package com.recall.app.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.MediaItemEntity
import com.recall.app.core.worker.IndexingPipelineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val indexingPipelineManager: IndexingPipelineManager,
) : ViewModel() {
    val mediaItems: StateFlow<List<MediaItemEntity>> = mediaItemDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaCount: StateFlow<Int> = mediaItemDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val indexedCount: StateFlow<Int> = mediaItemDao.observeIndexedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isPipelineActive: StateFlow<Boolean> = indexingPipelineManager.observePipelineStatus()
        .map { workInfos ->
            workInfos.any { info ->
                info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _userRefresh = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = combine(
        _userRefresh,
        isPipelineActive,
    ) { userRefresh, pipelineActive -> userRefresh || pipelineActive }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            isPipelineActive.collect { active ->
                if (!active) {
                    _userRefresh.value = false
                }
            }
        }
    }

    fun refresh() {
        _userRefresh.value = true
        indexingPipelineManager.startFullIndexing()
    }
}
