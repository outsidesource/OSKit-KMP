package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * [routeTransition] is a convenience delegate function to help implement [IAnimatedRoute]
 *
 * ```
 * sealed class Route : IRoute {
 *     object Home : Route(), IAnimatedRoute by routeTransition(SlideRouteTransition)
 * }
 * ```
 */
fun routeTransition(transition: IRouteTransition): IAnimatedRoute {
    return object : IAnimatedRoute {
        override val transition = transition
    }
}

/**
 * [ComposeRouteTransition] defines a route transition
 *
 * @param [enter] The animation for incoming content during a push()
 * @param [exit] The animation for the outgoing content during a push()
 * @param [popEnter] The animation for incoming content during a pop()
 * @param [popExit] The animation for outgoing content during a pop()
 */
@ExperimentalAnimationApi
data class ComposeRouteTransition(
    val enter: AnimatedContentScope<RouteStackEntry>.(density: Density) -> EnterTransition,
    val exit: AnimatedContentScope<RouteStackEntry>.(density: Density) -> ExitTransition,
    val popEnter: AnimatedContentScope<RouteStackEntry>.(density: Density) -> EnterTransition,
    val popExit: AnimatedContentScope<RouteStackEntry>.(density: Density) -> ExitTransition,
) : IRouteTransition

private val easeIn = CubicBezierEasing(.17f, .67f, .83f, .67f)

@ExperimentalAnimationApi
val DefaultRouteTransition = ComposeRouteTransition(
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
val HorizontalSlideRouteTransition = ComposeRouteTransition(
    enter = { slideInHorizontally(tween(300)) { it } },
    exit = { fadeOut(tween(300), targetAlpha = .5f) },
    popEnter = { fadeIn(tween(300)) },
    popExit = { slideOutHorizontally(tween(300, easing = easeIn)) { it } + fadeOut(tween(300), targetAlpha = .5f) },
)

@ExperimentalAnimationApi
val ScaleRouteTransition = ComposeRouteTransition(
    enter = { fadeIn(tween(300), 0f) + scaleIn(tween(300), initialScale = .9f) },
    exit = { fadeOut(tween(300), 0f) },
    popEnter = { scaleIn(tween(300), initialScale = 1.1f) + fadeIn(tween(300), 0f) },
    popExit = { fadeOut(tween(300), .99f) },
)

@ExperimentalAnimationApi
val NoRouteTransition = ComposeRouteTransition(
    enter = { EnterTransition.None },
    exit = { ExitTransition.None },
    popEnter = { EnterTransition.None },
    popExit = { ExitTransition.None },
)
