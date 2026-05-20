package com.recall.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.recall.app.feature.detail.MediaDetailScreen
import com.recall.app.feature.onboarding.OnboardingScreen
import com.recall.app.feature.search.SearchScreen
import com.recall.app.feature.settings.SettingsScreen
import com.recall.app.feature.timeline.TimelineScreen

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
        composable(RecallRoute.SEARCH) {
            SearchScreen(
                onMediaClick = { mediaId ->
                    navController.navigateToDetail(mediaId)
                },
            )
        }
        composable(RecallRoute.TIMELINE) {
            TimelineScreen(
                onMediaClick = { mediaId ->
                    navController.navigateToDetail(mediaId)
                },
            )
        }
        composable(RecallRoute.SETTINGS) {
            SettingsScreen()
        }
        composable(
            route = RecallRoute.DETAIL,
            arguments = listOf(
                navArgument(RecallRoute.MEDIA_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) {
            MediaDetailScreen(
                onNavigateBack = { navController.popBackFromDetail() },
            )
        }
        composable(RecallRoute.ONBOARDING) {
            OnboardingScreen(
                onOnboardingComplete = onOnboardingComplete,
            )
        }
    }
}
