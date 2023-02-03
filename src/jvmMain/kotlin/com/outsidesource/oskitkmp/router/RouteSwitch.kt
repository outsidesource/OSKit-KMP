package com.outsidesource.oskitkmp.router

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*

/**
 * [RouteSwitch] is the primary means of using a [Router] in a composable. [RouteSwitch] will automatically subscribe
 * to the passed in [Router] and update when the [Router] updates.
 *
 * @param [router] The [Router] to listen to.
 *
 * @param [content] The composable content to switch between routes. The current route to render is provided as the
 * parameter of the block.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RouteSwitch(
    router: IRouter,
    content: @Composable (route: IRoute) -> Unit
) {
    val routeDestroyedEffectHolder = remember { RouteDestroyedEffectHolder() }
    val currentRoute by router.routeFlow.collectAsState()

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = createComposeRouteTransition()
    ) { state ->
        if (transition.currentState != transition.targetState) {
            router.markTransitionStatus(RouteTransitionStatus.Running)
        } else {
            router.markTransitionStatus(RouteTransitionStatus.Completed)
        }

        CompositionLocalProvider(
            localRouteDestroyedEffectHolder provides routeDestroyedEffectHolder,
            localRouter provides router,
            LocalRoute provides state,
        ) {
            content(state.route)
        }
    }
}
