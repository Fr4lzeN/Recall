package com.recall.app.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.recall.app.core.designsystem.component.MediaGridItem
import com.recall.app.core.designsystem.component.RecallTopBar
import com.recall.app.core.designsystem.theme.RecallTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RecallTopBar(title = "Timeline")
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(timelinePlaceholderIds) { id ->
                MediaGridItem(
                    durationLabel = if (id % 3 == 0) "0:${30 + id}" else null,
                    onClick = { onMediaClick("timeline_$id") },
                )
            }
        }
    }
}

private val timelinePlaceholderIds = (1..24).toList()

@Preview
@Composable
private fun TimelineScreenPreview() {
    RecallTheme {
        TimelineScreen(onMediaClick = {})
    }
}
