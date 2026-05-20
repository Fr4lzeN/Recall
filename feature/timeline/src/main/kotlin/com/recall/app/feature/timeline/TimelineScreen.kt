package com.recall.app.feature.timeline

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.recall.app.core.designsystem.component.EmptyState
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.theme.RecallTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val mediaCount by viewModel.mediaCount.collectAsStateWithLifecycle()
    val indexedCount by viewModel.indexedCount.collectAsStateWithLifecycle()
    val isPipelineActive by viewModel.isPipelineActive.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val timelineEntries = remember(mediaItems) { buildTimelineEntries(mediaItems) }
    val isIndexingInProgress = indexedCount < mediaCount && mediaCount > 0
    val isScanning = mediaCount == 0 && isPipelineActive

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RecallTopBar(
                title = "Timeline",
                actions = {
                    if (isIndexingInProgress) {
                        Text(
                            text = "Indexing $indexedCount/$mediaCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isScanning || (isPipelineActive && mediaCount == 0)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                when {
                    isScanning -> {
                        EmptyState(
                            title = "Scanning your gallery…",
                            description = "We're finding photos and videos on your device. This may take a moment.",
                            icon = Icons.Outlined.PhotoLibrary,
                        )
                    }
                    mediaCount == 0 -> {
                        EmptyState(
                            title = "No photos found",
                            description = "Grant access in Settings to see your library here.",
                            icon = Icons.Outlined.Settings,
                            animateIcon = false,
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = timelineEntries,
                                key = { entry ->
                                    when (entry) {
                                        is TimelineEntry.DateHeader -> "header_${entry.dateKey}"
                                        is TimelineEntry.Media -> entry.item.id
                                    }
                                },
                                span = { entry ->
                                    when (entry) {
                                        is TimelineEntry.DateHeader -> GridItemSpan(maxLineSpan)
                                        is TimelineEntry.Media -> GridItemSpan(1)
                                    }
                                },
                            ) { entry ->
                                when (entry) {
                                    is TimelineEntry.DateHeader -> {
                                        Text(
                                            text = entry.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 8.dp),
                                        )
                                    }
                                    is TimelineEntry.Media -> {
                                        TimelineMediaItem(
                                            mediaItem = entry.item,
                                            onClick = { onMediaClick(entry.item.id.toString()) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineMediaItem(
    mediaItem: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVideo = mediaItem.mimeType.startsWith("video/")
    val durationLabel = mediaItem.duration?.let { formatDuration(it) }
    val imageDescription = buildString {
        append(mediaItem.displayName)
        if (isVideo) append(", video")
        if (!mediaItem.isIndexed) append(", indexing")
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                contentDescription = imageDescription
            },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = Uri.parse(mediaItem.uri),
                contentDescription = imageDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video indicator",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                }
            }
            if (durationLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            if (!mediaItem.isIndexed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "Indexing",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
    }
}

private sealed interface TimelineEntry {
    data class DateHeader(val dateKey: String, val label: String) : TimelineEntry
    data class Media(val item: MediaItemEntity) : TimelineEntry
}

private fun buildTimelineEntries(items: List<MediaItemEntity>): List<TimelineEntry> {
    val formatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    val grouped = items.groupBy { item ->
        val millis = item.sortDateMillis()
        formatter.format(Date(millis))
    }
    return buildList {
        grouped.forEach { (dateLabel, groupItems) ->
            add(TimelineEntry.DateHeader(dateKey = dateLabel, label = dateLabel))
            groupItems.forEach { item ->
                add(TimelineEntry.Media(item))
            }
        }
    }
}

private fun MediaItemEntity.sortDateMillis(): Long {
    val raw = dateTaken ?: dateAdded
    return if (dateTaken != null) raw else raw * 1000L
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Preview
@Composable
private fun TimelineScreenPreview() {
    RecallTheme {
        TimelineScreen(onMediaClick = {})
    }
}
