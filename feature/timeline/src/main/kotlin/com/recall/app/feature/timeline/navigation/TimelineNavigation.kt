package com.recall.app.feature.timeline.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.recall.app.feature.timeline.TimelineScreen

object TimelineRoute {
    const val ROUTE = "timeline"
}

fun NavGraphBuilder.timelineScreen(
    onMediaClick: (String) -> Unit,
) {
    composable(TimelineRoute.ROUTE) {
        TimelineScreen(onMediaClick = onMediaClick)
    }
}
