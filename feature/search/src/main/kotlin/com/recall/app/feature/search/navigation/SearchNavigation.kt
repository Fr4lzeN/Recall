package com.recall.app.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.recall.app.feature.search.SearchScreen

object SearchRoute {
    const val ROUTE = "search"
}

fun NavGraphBuilder.searchScreen(
    onMediaClick: (String) -> Unit,
) {
    composable(SearchRoute.ROUTE) {
        SearchScreen(onMediaClick = onMediaClick)
    }
}
