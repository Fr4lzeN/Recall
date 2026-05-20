package com.recall.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                ListItem(
                    headlineContent = { Text("Model profile") },
                    supportingContent = { Text(uiState.modelProfile) },
                    leadingContent = {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                    },
                    modifier = Modifier.clickable { },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Storage") },
                    supportingContent = { Text("Used: ${uiState.storageUsed}") },
                    leadingContent = {
                        Icon(Icons.Outlined.SdStorage, contentDescription = null)
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Re-index library") },
                    supportingContent = {
                        Text(
                            if (uiState.isReindexing) "Indexing in progress…"
                            else "Rebuild search index from your media",
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    },
                    modifier = Modifier.clickable { },
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    RecallTheme {
        SettingsScreen()
    }
}
