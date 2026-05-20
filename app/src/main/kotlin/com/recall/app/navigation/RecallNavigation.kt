package com.recall.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

fun NavController.navigateToSearch(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(RecallRoute.SEARCH) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
        builder()
    }
}

fun NavController.navigateToTimeline(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(RecallRoute.TIMELINE) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
        builder()
    }
}

fun NavController.navigateToSettings(builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(RecallRoute.SETTINGS) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
        builder()
    }
}

fun NavController.navigateToDetail(mediaId: String) {
    navigate(RecallRoute.detailRoute(mediaId)) {
        launchSingleTop = true
    }
}

fun NavController.navigateToOnboarding() {
    navigate(RecallRoute.ONBOARDING) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateToDirectoryExclusions() {
    navigate(RecallRoute.DIRECTORY_EXCLUSIONS) {
        launchSingleTop = true
    }
}

fun NavController.popBackFromDetail() {
    popBackStack()
}
