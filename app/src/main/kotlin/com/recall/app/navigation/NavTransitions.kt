package com.recall.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.recall.app.feature.detail.navigation.DetailRoute
import com.recall.app.feature.search.navigation.SearchRoute
import com.recall.app.feature.timeline.navigation.TimelineRoute

private const val TRANSITION_DURATION_MS = 300

private val topLevelRoutes = setOf(
    SearchRoute.ROUTE,
    TimelineRoute.ROUTE,
    RecallRoute.SETTINGS,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTopLevel(
    entry: NavBackStackEntry,
): Boolean = entry.destination.route in topLevelRoutes

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isDetail(
    entry: NavBackStackEntry,
): Boolean = entry.destination.route?.startsWith("detail") == true ||
    entry.destination.route?.startsWith(DetailRoute.ROUTE.substringBefore("/")) == true

fun AnimatedContentTransitionScope<NavBackStackEntry>.topLevelEnter(): EnterTransition =
    fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.topLevelExit(): ExitTransition =
    fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.detailEnter(): EnterTransition =
    slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(TRANSITION_DURATION_MS),
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.detailExit(): ExitTransition =
    slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(TRANSITION_DURATION_MS),
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS),
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS),
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS),
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(TRANSITION_DURATION_MS),
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnter(): EnterTransition =
    when {
        isDetail(targetState) -> detailEnter()
        isTopLevel(initialState) && isTopLevel(targetState) -> topLevelEnter()
        else -> forwardEnter()
    }

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExit(): ExitTransition =
    when {
        isDetail(targetState) -> forwardExit()
        isTopLevel(initialState) && isTopLevel(targetState) -> topLevelExit()
        else -> forwardExit()
    }

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnter(): EnterTransition =
    when {
        isDetail(initialState) -> topLevelEnter()
        isTopLevel(targetState) -> topLevelEnter()
        else -> backEnter()
    }

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExit(): ExitTransition =
    when {
        isDetail(initialState) -> detailExit()
        isTopLevel(targetState) -> topLevelExit()
        else -> backExit()
    }

fun detailEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { detailEnter() }

fun detailExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { forwardExit() }

fun detailPopEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { topLevelEnter() }

fun detailPopExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { detailExit() }

fun topLevelEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { topLevelEnter() }

fun topLevelExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { topLevelExit() }

fun topLevelPopEnterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { topLevelEnter() }

fun topLevelPopExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { topLevelExit() }
