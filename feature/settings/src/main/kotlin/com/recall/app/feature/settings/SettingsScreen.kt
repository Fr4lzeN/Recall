package com.recall.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    val progress = if (totalMedia > 0) indexedMedia.toFloat() / totalMedia else 0f

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
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = if (isIndexing) {
                                    "Indexing in progress…"
                                } else if (totalMedia == 0) {
                                    "Scan your library to begin indexing"
                                } else {
                                    "${(progress * 100).toInt()}% complete"
                                },
                            )
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.Sync, contentDescription = null)
                    },
                )
            }

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
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Model file") },
                    supportingContent = { Text(viewModel.activeProfile.modelFileName) },
                )
            }

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
                        Icon(Icons.Outlined.Memory, contentDescription = null)
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("CPU cores") },
                    supportingContent = { Text("${viewModel.deviceInfo.cpuCores}") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Android API") },
                    supportingContent = { Text("API ${viewModel.deviceInfo.androidVersion}") },
                    leadingContent = {
                        Icon(Icons.Outlined.PhoneAndroid, contentDescription = null)
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
                )
            }

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
                        Icon(Icons.Outlined.SdStorage, contentDescription = null)
                    },
                )
            }

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
                        onClick = viewModel::reindexAll,
                        enabled = !isIndexing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Text("Re-index All")
                        }
                    }
                    OutlinedButton(
                        onClick = viewModel::clearIndex,
                        enabled = !isIndexing && indexedMedia > 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                            Text("Clear Index")
                        }
                    }
                }
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
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
