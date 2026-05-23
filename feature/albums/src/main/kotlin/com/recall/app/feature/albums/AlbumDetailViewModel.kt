package com.recall.app.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.core.database.dao.MediaItemDao
import com.recall.app.core.database.entity.MediaItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val mediaItemDao: MediaItemDao,
) : ViewModel() {

    private val _mediaItems = MutableStateFlow<List<MediaItemEntity>>(emptyList())
    val mediaItems: StateFlow<List<MediaItemEntity>> = _mediaItems.asStateFlow()

    fun loadAlbum(mediaIds: List<Long>) {
        viewModelScope.launch {
            _mediaItems.value = mediaItemDao.getByIds(mediaIds)
        }
    }
}
