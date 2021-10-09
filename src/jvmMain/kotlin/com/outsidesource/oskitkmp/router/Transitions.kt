package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
interface IAnimatedRoute {
    val transition: RouteTransition
}

@OptIn(ExperimentalAnimationApi::class)
fun routeTransition(transition: RouteTransition): IAnimatedRoute {
    return object : IAnimatedRoute {
        override val transition = transition
    }
}

@ExperimentalAnimationApi
data class RouteTransition(
    val enter: AnimatedContentScope<RouteStackEntry>.(density: Density) -> EnterTransition,
    val exit: AnimatedContentScope<RouteStackEntry>.(density: Density) -> ExitTransition,
    val popEnter: AnimatedContentScope<RouteStackEntry>.(density: Density) -> EnterTransition,
    val popExit: AnimatedContentScope<RouteStackEntry>.(density: Density) -> ExitTransition,
)


@ExperimentalAnimationApi
val DefaultRouteTransition = RouteTransition(
    enter = {
        val offsetY = with(it) { -25.dp.toPx() }.toInt()
        fadeIn(0f, tween(400)) + slideIn({ IntOffset(0, offsetY) }, tween(400))
    },
    exit = { fadeOut(0f, tween(400)) },
    popEnter = { fadeIn(0f, tween(400)) },
    popExit = {
        val offsetY = with(it) { -25.dp.toPx() }.toInt()
        slideOut({ IntOffset(0, offsetY) }, tween(400)) + fadeOut(0f, tween(400))
    },
)

@ExperimentalAnimationApi
val SlideRouteTransition = RouteTransition(
    enter = { slideInHorizontally({ it }, tween(400)) + fadeIn(.99f, tween(400)) },
    exit = { slideOutHorizontally({ -it }, tween(400)) + fadeOut(.99f, tween(400)) },
    popEnter = { slideInHorizontally({ -it }, tween(400)) + fadeIn(.99f, tween(400)) },
    popExit = { slideOutHorizontally({ it }, tween(400)) + fadeOut(.99f, tween(400)) },
)