package com.outsidesource.oskitkmp.router

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import com.outsidesource.oskitkmp.coordinator.Coordinator

/**
 * [RouteSwitch] is the primary means of using a [Coordinator] in a composable. [RouteSwitch] will automatically subscribe
 * to the passed in [Coordinator] and update when the [Router] updates.
 *
 * @param [coordinator] The [Coordinator] to listen to.
 *
 * @param [content] The composable content to switch between routes. The current route to render is provided as the
 * parameter of the block.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RouteSwitch(
    coordinator: Coordinator,
    content: @Composable (route: IRoute) -> Unit
) {
    val currentRoute by coordinator.router.routeFlow.collectAsState()

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = createComposeRouteTransition()
    ) { state ->
        if (transition.currentState != transition.targetState) {
            coordinator.router.markTransitionStatus(RouteTransitionStatus.Running)
        } else {
            coordinator.router.markTransitionStatus(RouteTransitionStatus.Completed)
        }

        CompositionLocalProvider(
            localRouter provides coordinator.router,
            LocalRoute provides state,
        ) {
            content(state.route)
        }
    }
}
