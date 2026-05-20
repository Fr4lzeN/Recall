package com.recall.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Search(
        route = RecallRoute.SEARCH,
        label = "Search",
        icon = Icons.Default.Search,
    ),
    Timeline(
        route = RecallRoute.TIMELINE,
        label = "Timeline",
        icon = Icons.Default.History,
    ),
    Settings(
        route = RecallRoute.SETTINGS,
        label = "Settings",
        icon = Icons.Default.Settings,
    ),
}
