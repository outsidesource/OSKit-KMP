package com.outsidesource.oskitkmp.router

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

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
    defaultTransition: RouteTransition = DefaultRouteTransition,
    content: @Composable (route: IRoute) -> Unit,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val routeDestroyedEffectHolder = remember { RouteDestroyedEffectHolder() }
    val currentRoute by router.routeFlow.collectAsState()

    BackHandler(enabled = router.hasBackStack()) { router.pop() }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = createRouteTransition(defaultTransition)
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
            RouteDestroyedEffect("com.outsidesource.oskitkmp.router.RouteSwitch") {
                saveableStateHolder.removeState(state.id)
            }
            saveableStateHolder.SaveableStateProvider(state.id) {
                content(state.route)
            }
        }
    }
}
