package com.recall.app.feature.albums

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.recall.app.core.database.entity.MediaItemEntity
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.theme.RecallTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumIndex: Int,
    onNavigateBack: () -> Unit,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    albumsViewModel: AlbumsViewModel = hiltViewModel(),
    detailViewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val album = albumsViewModel.getAlbum(albumIndex)
    val mediaItems by detailViewModel.mediaItems.collectAsStateWithLifecycle()

    LaunchedEffect(album) {
        album?.let { detailViewModel.loadAlbum(it.mediaIds) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RecallTopBar(
                title = album?.name ?: "Album",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            album == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            mediaItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    items(
                        items = mediaItems,
                        key = { it.id },
                    ) { item ->
                        AlbumPhotoTile(
                            mediaItem = item,
                            onClick = { onMediaClick(item.id.toString()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumPhotoTile(
    mediaItem: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = mediaItem.displayName
            },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        AsyncImage(
            model = Uri.parse(mediaItem.uri),
            contentDescription = mediaItem.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Preview
@Composable
private fun AlbumDetailScreenPreview() {
    RecallTheme {
        AlbumDetailScreen(
            albumIndex = 0,
            onNavigateBack = {},
            onMediaClick = {},
        )
    }
}
