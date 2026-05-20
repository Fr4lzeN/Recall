package com.recall.app.feature.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.theme.RecallTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
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

    if (showReindexDialog) {
        AlertDialog(
            onDismissRequest = { showReindexDialog = false },
            title = { Text("Re-index all photos?") },
            text = {
                Text(
                    "This will re-scan and re-embed your entire library. " +
                        "It may take a while and use battery while running.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReindexDialog = false
                        viewModel.reindexAll()
                    },
                ) {
                    Text("Re-index")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReindexDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear search index?") },
            text = {
                Text(
                    "This removes all embeddings from the index. Your photos stay on device, " +
                        "but search won't work until indexing runs again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearIndex()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                ListItem(
                    headlineContent = { Text("$indexedMedia of $totalMedia items indexed") },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isIndexing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Text(
                                text = when {
                                    isIndexing -> "Indexing in progress…"
                                    totalMedia == 0 -> "Scan your library to begin indexing"
                                    else -> "${(progress * 100).toInt()}% complete"
                                },
                            )
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Indexing status",
                        )
                    },
                )
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Model Profile")
            }
            item {
                ListItem(
                    headlineContent = { Text(viewModel.activeProfile.name) },
                    supportingContent = {
                        Text(
                            "${viewModel.activeProfile.dimensions} dimensions · " +
                                viewModel.activeProfile.quantization.name,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Model profile",
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Model file") },
                    supportingContent = { Text(viewModel.activeProfile.modelFileName) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.SmartToy,
                            contentDescription = "Model file",
                        )
                    },
                )
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Device Info")
            }
            item {
                ListItem(
                    headlineContent = { Text("RAM") },
                    supportingContent = {
                        Text(
                            "${viewModel.deviceInfo.availableRamMb} MB available / " +
                                "${viewModel.deviceInfo.totalRamMb} MB total",
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Memory,
                            contentDescription = "RAM",
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("CPU cores") },
                    supportingContent = { Text("${viewModel.deviceInfo.cpuCores}") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.DeveloperBoard,
                            contentDescription = "CPU cores",
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Android API") },
                    supportingContent = { Text("API ${viewModel.deviceInfo.androidVersion}") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = "Android version",
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("NNAPI support") },
                    supportingContent = {
                        Text(
                            if (viewModel.deviceInfo.supportsNnapi) "Supported" else "Not supported",
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Android,
                            contentDescription = "NNAPI support",
                        )
                    },
                )
            }

            item { SettingsSectionDivider() }

            item {
                SettingsSectionHeader(title = "Storage")
            }
            item {
                ListItem(
                    headlineContent = { Text("Free disk space") },
                    supportingContent = {
                        Text(formatDiskSpace(viewModel.deviceInfo.freeDiskMb))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.SdStorage,
                            contentDescription = "Storage",
                        )
                    },
                )
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
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                            )
                            Text("Re-index All")
                        }
                    }
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        enabled = !isIndexing && indexedMedia > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
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
                ListItem(
                    headlineContent = { Text("Recall") },
                    supportingContent = { Text("Version $versionName") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About Recall",
                        )
                    },
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
        text = title,
        style = MaterialTheme.typography.titleSmall,
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
        color = MaterialTheme.colorScheme.outlineVariant,
    )
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
