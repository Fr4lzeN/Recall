package com.recall.app.feature.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.recall.app.core.database.entity.MediaItemEntity
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.theme.RecallTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    val mediaItem by viewModel.mediaItem.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RecallTopBar(
                title = mediaItem?.displayName ?: "Media",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val item = mediaItem) {
            null -> {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    MediaPreview(
                        mediaItem = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    MediaMetadataPanel(
                        mediaItem = item,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaPreview(
    mediaItem: MediaItemEntity,
    modifier: Modifier = Modifier,
) {
    val isVideo = mediaItem.mimeType.startsWith("video/")
    val previewDescription = if (isVideo) {
        "Video preview: ${mediaItem.displayName}"
    } else {
        "Photo preview: ${mediaItem.displayName}"
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = previewDescription },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Uri.parse(mediaItem.uri),
            contentDescription = previewDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        if (isVideo) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun MediaMetadataPanel(
    mediaItem: MediaItemEntity,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IndexingStatusBadge(isIndexed = mediaItem.isIndexed)
            MetadataRow(label = "Filename", value = mediaItem.displayName)
            MetadataRow(label = "Date", value = formatMediaDate(mediaItem))
            MetadataRow(
                label = "Dimensions",
                value = "${mediaItem.width} × ${mediaItem.height}",
            )
            MetadataRow(label = "Size", value = formatFileSize(mediaItem.size))
            MetadataRow(label = "Type", value = mediaItem.mimeType)
            val duration = mediaItem.duration
            if (duration != null && mediaItem.mimeType.startsWith("video/")) {
                MetadataRow(label = "Duration", value = formatDuration(duration))
            }
        }
    }
}

@Composable
private fun IndexingStatusBadge(
    isIndexed: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = if (isIndexed) "Indexed" else "Not indexed"
    val containerColor = if (isIndexed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isIndexed) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatMediaDate(mediaItem: MediaItemEntity): String {
    val dateTaken = mediaItem.dateTaken
    val millis = if (dateTaken != null) {
        dateTaken
    } else {
        mediaItem.dateAdded * 1000L
    }
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview
@Composable
private fun MediaDetailScreenPreview() {
    RecallTheme {
        MediaDetailScreen(onNavigateBack = {})
    }
}
