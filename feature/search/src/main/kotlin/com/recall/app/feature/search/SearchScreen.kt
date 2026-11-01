package com.recall.app.feature.search

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
            if (uiState.query.isNotBlank() && uiState.error == null) {
                SearchFilterControls(
                    minSimilarity = uiState.minSimilarity,
                    onMinSimilarityChange = viewModel::onMinSimilarityChange,
                    onShowNearest = viewModel::showNearestPhotos,
                )
            }
            when {
                uiState.error != null -> {
                    ErrorState(
                        title = "Search failed",
                        description = uiState.error.orEmpty(),
                        onRetry = viewModel::retrySearch,
                    )
                }
                uiState.query.isBlank() -> {
                    if (uiState.indexedCount == 0 && uiState.totalCount > 0) {
                        EmptyState(
                            title = "Indexing not started",
                            description = "Your photos haven't been indexed yet. Go to Settings to start.",
                            icon = Icons.Outlined.PhotoLibrary,
                        )
                    } else {
                        EmptyState(
                            title = "Search your memories",
                            description = buildIndexedCountDescription(
                                indexedCount = uiState.indexedCount,
                                totalCount = uiState.totalCount,
                            ),
                            icon = Icons.Outlined.Search,
                        )
                    }
                }
                uiState.indexedCount == 0 -> {
                    EmptyState(
                        title = "Indexing not started",
                        description = "Your photos haven't been indexed yet. Go to Settings to start.",
                        icon = Icons.Outlined.PhotoLibrary,
                    )
                }
                uiState.isSearching -> {
                    LoadingState()
                }
                uiState.results.isEmpty() -> {
                    EmptyState(
                        title = "No photos match your search",
                        description = "Try another description or lower the search filter to include more photos.",
                        icon = Icons.Outlined.ImageNotSupported,
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
    val imageDescription = item.displayName

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
                model = Uri.parse(item.uri),
                contentDescription = imageDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun SearchFilterControls(
    minSimilarity: Float,
    onMinSimilarityChange: (Float) -> Unit,
    onShowNearest: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Search strictness", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onShowNearest) {
                Text(if (minSimilarity < 0f) "Showing nearest photos" else "Show nearest photos")
            }
        }
        Slider(
            value = minSimilarity.coerceIn(0f, 0.5f),
            onValueChange = onMinSimilarityChange,
            valueRange = 0f..0.5f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Move left to include more photos. Nearest photos may include unrelated images.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
