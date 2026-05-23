package com.recall.app.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.recall.app.core.designsystem.component.RecallConfirmDialog
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.component.LoadingState
import com.recall.app.core.designsystem.theme.DarkExcludedRowBg
import com.recall.app.core.designsystem.theme.DarkSummaryCardActiveBg
import com.recall.app.core.designsystem.theme.DarkSummaryCardActiveBorder
import com.recall.app.core.designsystem.theme.DarkSummaryCardBg
import com.recall.app.core.designsystem.theme.DarkTilePlaceholder
import com.recall.app.core.designsystem.theme.RecallTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryExclusionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DirectoryExclusionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showReindexDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            delay(3000)
            snackbarMessage = null
        }
    }

    if (showReindexDialog) {
        RecallConfirmDialog(
            title = "Re-index with new exclusions?",
            body = "Excluded folders will be removed from the search index. This may take a while.",
            confirmLabel = "Re-index",
            dismissLabel = "Later",
            onConfirm = {
                showReindexDialog = false
                viewModel.applyAndReindex {
                    snackbarMessage = "Re-indexing started with new exclusions."
                    onNavigateBack()
                }
            },
            onDismiss = { showReindexDialog = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RecallTopBar(
                title = "Excluded Folders",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                uiState.folders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No media folders found on this device.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = if (uiState.hasChanges) 140.dp else 16.dp,
                        ),
                    ) {
                        item {
                            SummaryCard(
                                excludedCount = uiState.excludedCount,
                                skippedItems = uiState.skippedItems,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                        items(uiState.folders, key = { it.bucketId }) { folder ->
                            FolderRow(
                                folder = folder,
                                onToggle = { viewModel.toggleFolder(folder.bucketId) },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            if (uiState.hasChanges) {
                StickyApplyBar(
                    onApplyReindex = { showReindexDialog = true },
                    onApplyOnly = {
                        viewModel.applyWithoutReindex {
                            snackbarMessage = "Exclusions saved. Re-index from Settings when ready."
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            snackbarMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (uiState.hasChanges) 140.dp else 24.dp,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    excludedCount: Int,
    skippedItems: Int,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (excludedCount > 0) DarkSummaryCardActiveBg else DarkSummaryCardBg
    val borderColor = if (excludedCount > 0) {
        DarkSummaryCardActiveBorder
    } else {
        MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOff,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = if (excludedCount == 0) {
                    "No folders excluded"
                } else {
                    "$excludedCount folder${if (excludedCount > 1) "s" else ""} excluded"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (excludedCount == 0) {
                    "Your entire library will be indexed."
                } else {
                    "${String.format("%,d", skippedItems)} items will be skipped on next index"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "Changes take effect on the next indexing run.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FolderRow(
    folder: FolderUiModel,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val excluded = !folder.included
    val rowBackground by animateColorAsState(
        targetValue = if (excluded) DarkExcludedRowBg else Color.Transparent,
        label = "folder_row_bg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (excluded) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.error),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FolderThumbnailCollage(
                coverUris = folder.coverUris,
                excluded = excluded,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = "${String.format("%,d", folder.itemCount)} photos & videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = folder.included,
                onCheckedChange = { onToggle() },
                modifier = Modifier.width(48.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun FolderThumbnailCollage(
    coverUris: List<String>,
    excluded: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkTilePlaceholder),
    ) {
        if (coverUris.isEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    ThumbnailCell(uri = coverUris.getOrNull(0), modifier = Modifier.weight(1f))
                    ThumbnailCell(uri = coverUris.getOrNull(1), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.weight(1f)) {
                    ThumbnailCell(uri = coverUris.getOrNull(2), modifier = Modifier.weight(1f))
                    ThumbnailCell(uri = coverUris.getOrNull(3), modifier = Modifier.weight(1f))
                }
            }
        }
        if (excluded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ThumbnailCell(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(0.5.dp)
            .clip(RoundedCornerShape(2.dp)),
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun StickyApplyBar(
    onApplyReindex: () -> Unit,
    onApplyOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onApplyReindex,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text("Apply & Re-index")
            }
        }
        TextButton(
            onClick = onApplyOnly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Apply without re-indexing",
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun DirectoryExclusionScreenPreview() {
    RecallTheme {
        DirectoryExclusionScreen(onNavigateBack = {})
    }
}
