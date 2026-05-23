package com.recall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.recall.app.core.designsystem.theme.DarkBottomNavBorder
import com.recall.app.feature.onboarding.OnboardingViewModel
import com.recall.app.navigation.RecallNavHost
import com.recall.app.navigation.RecallRoute
import com.recall.app.navigation.TopLevelDestination
import com.recall.app.navigation.navigateToAlbums
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

    val topLevelRoutes = TopLevelDestination.entries.map { it.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    val startDestination = if (uiState.shouldShowOnboarding) {
        RecallRoute.ONBOARDING
    } else {
        RecallRoute.SEARCH
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                RecallBottomBar(
                    currentRoute = currentDestination?.route,
                    onDestinationClick = { destination ->
                        when (destination) {
                            TopLevelDestination.Search -> navController.navigateToSearch()
                            TopLevelDestination.Albums -> navController.navigateToAlbums()
                            TopLevelDestination.Timeline -> navController.navigateToTimeline()
                            TopLevelDestination.Settings -> navController.navigateToSettings()
                        }
                    },
                )
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

@Composable
private fun RecallBottomBar(
    currentRoute: String?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkBottomNavBorder),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentRoute?.let { route ->
                    destination.route == route
                } == true
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { onDestinationClick(destination) }
                        .semantics {
                            role = Role.Tab
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(CircleShape)
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(24.dp),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
    }
}
