package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * [RouteTransition] defines a route transition
 *
 * @param [enter] The animation for incoming content during a push()
 * @param [exit] The animation for the outgoing content during a push()
 * @param [popEnter] The animation for incoming content during a pop()
 * @param [popExit] The animation for outgoing content during a pop()
 */
@ExperimentalAnimationApi
data class RouteTransition(
    val enter: AnimatedContentScope<RouteStackEntry>.(density: Density) -> EnterTransition,
    val exit: AnimatedContentScope<RouteStackEntry>.(density: Density) -> ExitTransition,
    val popEnter: AnimatedContentScope<RouteStackEntry>.(density: Density) -> EnterTransition,
    val popExit: AnimatedContentScope<RouteStackEntry>.(density: Density) -> ExitTransition,
) : IRouteTransition

private val easeIn = CubicBezierEasing(.17f, .67f, .83f, .67f)

/**
 * [DefaultRouteTransition] the default transition used if no other transition is supplied.
 */
@ExperimentalAnimationApi
val DefaultRouteTransition = RouteTransition(
    enter = {
        val offsetY = with(it) { -25.dp.toPx() }.toInt()
        fadeIn(tween(300), 0f) + slideIn(tween(300)) { IntOffset(0, offsetY) }
    },
    exit = { fadeOut(tween(300), 0f) },
    popEnter = { fadeIn(tween(300), 0f) },
    popExit = {
        val offsetY = with(it) { -25.dp.toPx() }.toInt()
        slideOut(tween(300)) { IntOffset(0, offsetY) } + fadeOut(tween(300), 0f)
    },
)

@ExperimentalAnimationApi
val HorizontalSlideRouteTransition = RouteTransition(
    enter = { slideInHorizontally(tween(300)) { it } },
    exit = { fadeOut(tween(300), targetAlpha = .5f) },
    popEnter = { fadeIn(tween(300)) },
    popExit = { slideOutHorizontally(tween(300, easing = easeIn)) { it } + fadeOut(tween(300), targetAlpha = .5f) },
)

@ExperimentalAnimationApi
val ScaleRouteTransition = RouteTransition(
    enter = { fadeIn(tween(300), 0f) + scaleIn(tween(300), initialScale = .9f) },
    exit = { fadeOut(tween(300), 0f) },
    popEnter = { scaleIn(tween(300), initialScale = 1.1f) + fadeIn(tween(300), 0f) },
    popExit = { fadeOut(tween(300), .99f) },
)
