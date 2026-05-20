package com.recall.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.recall.app.feature.detail.MediaDetailScreen
import com.recall.app.feature.detail.navigation.DetailRoute
import com.recall.app.feature.onboarding.OnboardingScreen
import com.recall.app.feature.search.SearchScreen
import com.recall.app.feature.search.navigation.SearchRoute
import com.recall.app.feature.settings.SettingsScreen
import com.recall.app.feature.timeline.TimelineScreen
import com.recall.app.feature.timeline.navigation.TimelineRoute

@Composable
fun RecallNavHost(
    navController: NavHostController,
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(
            route = SearchRoute.ROUTE,
            enterTransition = topLevelEnterTransition(),
            exitTransition = topLevelExitTransition(),
            popEnterTransition = topLevelPopEnterTransition(),
            popExitTransition = topLevelPopExitTransition(),
        ) {
            SearchScreen(
                onMediaClick = { mediaId ->
                    navController.navigateToDetail(mediaId)
                },
            )
        }
        composable(
            route = TimelineRoute.ROUTE,
            enterTransition = topLevelEnterTransition(),
            exitTransition = topLevelExitTransition(),
            popEnterTransition = topLevelPopEnterTransition(),
            popExitTransition = topLevelPopExitTransition(),
        ) {
            TimelineScreen(
                onMediaClick = { mediaId ->
                    navController.navigateToDetail(mediaId)
                },
            )
        }
        composable(
            route = RecallRoute.SETTINGS,
            enterTransition = topLevelEnterTransition(),
            exitTransition = topLevelExitTransition(),
            popEnterTransition = topLevelPopEnterTransition(),
            popExitTransition = topLevelPopExitTransition(),
        ) {
            SettingsScreen()
        }
        composable(
            route = DetailRoute.ROUTE,
            arguments = listOf(
                navArgument(DetailRoute.MEDIA_ID_ARG) {
                    type = NavType.StringType
                },
            ),
            enterTransition = detailEnterTransition(),
            exitTransition = detailExitTransition(),
            popEnterTransition = detailPopEnterTransition(),
            popExitTransition = detailPopExitTransition(),
        ) {
            MediaDetailScreen(
                onNavigateBack = { navController.popBackFromDetail() },
            )
        }
        composable(
            route = RecallRoute.ONBOARDING,
            enterTransition = { forwardEnter() },
            exitTransition = { forwardExit() },
            popEnterTransition = { backEnter() },
            popExitTransition = { backExit() },
        ) {
            OnboardingScreen(
                onOnboardingComplete = onOnboardingComplete,
            )
        }
    }
}
