package com.recall.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recall.app.core.designsystem.theme.RecallTheme

@Composable
fun MediaGridItem(
    modifier: Modifier = Modifier,
    similarityScore: Float? = null,
    durationLabel: String? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            if (durationLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            if (similarityScore != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${(similarityScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            if (durationLabel != null && similarityScore == null) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 24.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MediaGridItemPreview() {
    RecallTheme {
        MediaGridItem(
            similarityScore = 0.87f,
            durationLabel = "1:24",
        )
    }
}
