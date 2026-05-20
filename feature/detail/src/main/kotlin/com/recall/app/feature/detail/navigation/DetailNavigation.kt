package com.recall.app.feature.detail.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.recall.app.feature.detail.MediaDetailScreen

object DetailRoute {
    const val ROUTE = "detail/{mediaId}"
    const val MEDIA_ID_ARG = "mediaId"

    fun createRoute(mediaId: String): String = "detail/$mediaId"
}

fun NavGraphBuilder.detailScreen(
    onNavigateBack: () -> Unit,
) {
    composable(
        route = DetailRoute.ROUTE,
        arguments = listOf(
            navArgument(DetailRoute.MEDIA_ID_ARG) {
                type = NavType.StringType
            },
        ),
    ) {
        MediaDetailScreen(onNavigateBack = onNavigateBack)
    }
}
