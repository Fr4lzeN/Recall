package com.recall.app.feature.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.recall.app.core.designsystem.component.EmptyState
import com.recall.app.core.designsystem.component.ErrorState
import com.recall.app.core.designsystem.component.LoadingState
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
            when {
                uiState.error != null -> {
                    ErrorState(
                        title = "Search failed",
                        description = uiState.error.orEmpty(),
                        onRetry = viewModel::retrySearch,
                    )
                }
                uiState.query.isBlank() -> {
                    EmptyState(
                        title = "Search your memories",
                        description = buildIndexedCountDescription(
                            indexedCount = uiState.indexedCount,
                            totalCount = uiState.totalCount,
                        ),
                    )
                }
                uiState.isSearching -> {
                    LoadingState()
                }
                uiState.results.isEmpty() -> {
                    EmptyState(
                        title = "No results found",
                        description = "Try a different description — Recall matches photos by meaning, not exact filenames.",
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = uiState.results,
                            key = { it.mediaId },
                        ) { item ->
                            SearchResultGridItem(
                                item = item,
                                onClick = { onMediaClick(item.mediaId.toString()) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultGridItem(
    item: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = Uri.parse(item.uri),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${(item.score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

private fun buildIndexedCountDescription(indexedCount: Int, totalCount: Int): String {
    return if (totalCount > 0) {
        "$indexedCount of $totalCount photos indexed. Describe a photo or moment — Recall will find matching images in your library."
    } else {
        "Describe a photo or moment — Recall will find matching images once your library is indexed."
    }
}

@Preview
@Composable
private fun SearchScreenPreview() {
    RecallTheme {
        SearchScreen(onMediaClick = {})
    }
}
