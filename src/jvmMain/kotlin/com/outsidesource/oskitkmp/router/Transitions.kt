package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * [IAnimatedRoute] defines a route transition on an [IRoute]. For ease of use, the delegate function [routeTransition]
 * may be used.
 *
 * ```
 * sealed class Route : IRoute {
 *     data class Home : Route(), IAnimatedRoute by routeTransition(SlideRouteTransition)
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
interface IAnimatedRoute {
    val transition: RouteTransition
}


/**
 * [routeTransition] is a convenience delegate function to help implement [IAnimatedRoute]
 *
 * ```
 * sealed class Route : IRoute {
 *     data class Home : Route(), IAnimatedRoute by routeTransition(SlideRouteTransition)
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
fun routeTransition(transition: RouteTransition): IAnimatedRoute {
    return object : IAnimatedRoute {
        override val transition = transition
    }
}

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
)


/**
 * [DefaultRouteTransition] the default transition used if no other transition is supplied.
 */
@ExperimentalAnimationApi
val DefaultRouteTransition = RouteTransition(
    enter = {
        val offsetY = with(it) { -25.dp.toPx() }.toInt()
        fadeIn(tween(400), 0f) + slideIn(tween(400)) { IntOffset(0, offsetY) }
    },
    exit = { fadeOut(tween(400), 0f) },
    popEnter = { fadeIn(tween(400), 0f) },
    popExit = {
        val offsetY = with(it) { -25.dp.toPx() }.toInt()
        slideOut(tween(400)) { IntOffset(0, offsetY) } + fadeOut(tween(400), 0f)
    },
)

@ExperimentalAnimationApi
val HorizontalSlideRouteTransition = RouteTransition(
    enter = { slideInHorizontally(tween(400)) { it } + fadeIn(tween(400), .99f) },
    exit = { slideOutHorizontally(tween(400)) { -it } + fadeOut(tween(400), .99f) },
    popEnter = { slideInHorizontally(tween(400)) { -it } + fadeIn(tween(400), .99f) },
    popExit = { slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400), .99f) },
)