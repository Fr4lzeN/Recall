package com.recall.app.feature.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recall.app.core.designsystem.component.RecallConfirmDialog
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.theme.RecallTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToExclusions: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val totalMedia by viewModel.totalMedia.collectAsStateWithLifecycle()
    val indexedMedia by viewModel.indexedMedia.collectAsStateWithLifecycle()
    val isIndexing by viewModel.isIndexing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val progress = if (totalMedia > 0) indexedMedia.toFloat() / totalMedia else 0f
    val versionName = remember {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        }.getOrNull() ?: "0.0.0"
    }

    var showReindexDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val excludedCount = 0
    val skippedItems = 0

    if (showReindexDialog) {
        RecallConfirmDialog(
            title = "Re-index all photos?",
            body = "This will re-scan and re-embed your entire library. " +
                "It may take a while and use battery while running.",
            confirmLabel = "Re-index",
            dismissLabel = "Cancel",
            onConfirm = {
                showReindexDialog = false
                viewModel.reindexAll()
            },
            onDismiss = { showReindexDialog = false },
        )
    }

    if (showClearDialog) {
        RecallConfirmDialog(
            title = "Clear search index?",
            body = "This removes all embeddings from the index. Your photos stay on device, " +
                "but search won't work until indexing runs again.",
            confirmLabel = "Clear",
            dismissLabel = "Cancel",
            onConfirm = {
                showClearDialog = false
                viewModel.clearIndex()
            },
            onDismiss = { showClearDialog = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RecallTopBar(title = "Settings")
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                SettingsSectionHeader(title = "Indexing Status")
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$indexedMedia of $totalMedia items indexed",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isIndexing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline,
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Text(
                            text = when {
                                isIndexing -> "Indexing in progress…"
                                totalMedia == 0 -> "Scan your library to begin indexing"
                                else -> "${(progress * 100).toInt()}% complete"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Model Profile")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Tune,
                    headline = viewModel.activeProfile.name,
                    supporting = "${viewModel.activeProfile.dimensions} dimensions · " +
                        viewModel.activeProfile.quantization.name,
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.SmartToy,
                    headline = "Model file",
                    supporting = viewModel.activeProfile.modelFileName,
                )
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Device Info")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Memory,
                    headline = "RAM",
                    supporting = "${viewModel.deviceInfo.availableRamMb} MB available / " +
                        "${viewModel.deviceInfo.totalRamMb} MB total",
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.DeveloperBoard,
                    headline = "CPU cores",
                    supporting = "${viewModel.deviceInfo.cpuCores}",
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    headline = "Android API",
                    supporting = "API ${viewModel.deviceInfo.androidVersion}",
                )
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Android,
                    headline = "NNAPI support",
                    supporting = if (viewModel.deviceInfo.supportsNnapi) "Supported" else "Not supported",
                )
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Storage")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.SdStorage,
                    headline = "Free disk space",
                    supporting = formatDiskSpace(viewModel.deviceInfo.freeDiskMb),
                )
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Indexing Scope")
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToExclusions)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Excluded Directories",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (excludedCount > 0) {
                                "$excludedCount folders excluded · $skippedItems items skipped"
                            } else {
                                "No folders excluded — all media will be indexed."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Actions")
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showReindexDialog = true },
                        enabled = !isIndexing,
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
                            Text("Re-index All")
                        }
                    }
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        enabled = !isIndexing && indexedMedia > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text("Clear Index")
                        }
                    }
                }
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "About")
            }
            item {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    headline = "Recall",
                    supporting = "Version $versionName",
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsSectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    headline: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun formatDiskSpace(freeMb: Long): String = when {
    freeMb >= 1024 -> "%.1f GB free".format(freeMb / 1024.0)
    else -> "$freeMb MB free"
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    RecallTheme {
        SettingsScreen()
    }
}
