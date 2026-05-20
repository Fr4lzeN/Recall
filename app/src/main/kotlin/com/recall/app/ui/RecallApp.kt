package com.recall.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.recall.app.feature.onboarding.OnboardingViewModel
import com.recall.app.navigation.RecallNavHost
import com.recall.app.navigation.RecallRoute
import com.recall.app.navigation.TopLevelDestination
import com.recall.app.navigation.navigateToSearch
import com.recall.app.navigation.navigateToSettings
import com.recall.app.navigation.navigateToTimeline

@Composable
fun RecallApp(
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in TopLevelDestination.entries.map { it.route }

    val startDestination = if (uiState.shouldShowOnboarding) {
        RecallRoute.ONBOARDING
    } else {
        RecallRoute.SEARCH
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                when (destination) {
                                    TopLevelDestination.Search ->
                                        navController.navigateToSearch()
                                    TopLevelDestination.Timeline ->
                                        navController.navigateToTimeline()
                                    TopLevelDestination.Settings ->
                                        navController.navigateToSettings()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        RecallNavHost(
            navController = navController,
            startDestination = startDestination,
            onOnboardingComplete = {
                viewModel.markOnboardingComplete()
                navController.navigateToSearch {
                    popUpTo(RecallRoute.ONBOARDING) { inclusive = true }
                }
            },
            modifier = Modifier.padding(innerPadding),
        )
    }
}
