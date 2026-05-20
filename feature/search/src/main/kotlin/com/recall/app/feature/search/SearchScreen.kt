package com.recall.app.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recall.app.core.designsystem.component.EmptyState
import com.recall.app.core.designsystem.component.MediaGridItem
import com.recall.app.core.designsystem.component.RecallSearchBar
import com.recall.app.core.designsystem.theme.RecallTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            RecallSearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (uiState.query.isBlank()) {
                EmptyState(
                    title = "Search your memories",
                    description = "Describe a photo or moment — Recall will find matching images in your library.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(placeholderIds) { id ->
                        MediaGridItem(
                            similarityScore = 0.75f + (id % 20) * 0.01f,
                            onClick = { onMediaClick("media_$id") },
                        )
                    }
                }
            }
        }
    }
}

private val placeholderIds = (1..12).toList()

@Preview
@Composable
private fun SearchScreenPreview() {
    RecallTheme {
        SearchScreen(onMediaClick = {})
    }
}
