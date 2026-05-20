package com.recall.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.MediaItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaItemDao: MediaItemDao,
) : ViewModel() {
    private val mediaId: Long = checkNotNull(savedStateHandle.get<String>("mediaId")).toLong()

    val mediaItem: StateFlow<MediaItemEntity?> = flow { emit(mediaItemDao.getById(mediaId)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
