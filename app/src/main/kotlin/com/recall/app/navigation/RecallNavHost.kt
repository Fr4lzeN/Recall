package com.recall.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.recall.app.feature.detail.navigation.detailScreen
import com.recall.app.feature.onboarding.OnboardingScreen
import com.recall.app.feature.search.navigation.searchScreen
import com.recall.app.feature.settings.SettingsScreen
import com.recall.app.feature.timeline.navigation.timelineScreen

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
        searchScreen(
            onMediaClick = { mediaId ->
                navController.navigateToDetail(mediaId)
            },
        )
        timelineScreen(
            onMediaClick = { mediaId ->
                navController.navigateToDetail(mediaId)
            },
        )
        composable(RecallRoute.SETTINGS) {
            SettingsScreen()
        }
        detailScreen(
            onNavigateBack = { navController.popBackFromDetail() },
        )
        composable(RecallRoute.ONBOARDING) {
            OnboardingScreen(
                onOnboardingComplete = onOnboardingComplete,
            )
        }
    }
}
