package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.outsidesource.oskitkmp.extensions.disablePointerInput
import kotlinx.coroutines.CoroutineScope

/**
 * [RouteSwitch] is the primary means of using a [Router] in a composable. [RouteSwitch] will automatically subscribe
 * to the passed in [Router] and update when the [Router] updates.
 *
 * @param [router] The [Router] to listen to.
 *
 * @param [content] The composable content to switch between routes. The current route to render is provided as the
 * parameter of the block. Route transitions are supported by the provided [IRoute] also implementing [IAnimatedRoute].
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RouteSwitch(router: Router, content: @Composable (route: IRoute) -> Unit) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val currentRoute = remember { mutableStateOf(router.current) }
    val routeScopeHolder = remember { mutableMapOf<Int, CoroutineScope>() }

    DisposableEffect(Unit) {
        val routeChangeListener: RouteChangeListener = { currentRoute.value = it }
        router.subscribe(routeChangeListener)

        onDispose { router.unsubscribe(routeChangeListener) }
    }

    AnimatedContent(currentRoute.value, transitionSpec = createRouteTransition()) { state ->
        CompositionLocalProvider(
            LocalRouter provides router,
            LocalRoute provides state,
            LocalRouteScope provides routeScopeHolder.getOrPut(state.id) { createRouteScope() },
        ) {
            DisposableEffect(Unit) {
                router.setRouteViewStatus(state, RouteViewStatus.Visible)
                onDispose {
                    if (state.lifecycle != RouteLifecycle.Destroyed) {
                        router.setRouteViewStatus(state, RouteViewStatus.Disposed)
                    }
                }
            }

            RouteDestroyedEffect {
                routeScopeHolder.remove(state.id)
                saveableStateHolder.removeState(state.id)
            }
            saveableStateHolder.SaveableStateProvider(state.id) {
                Box(modifier = Modifier.fillMaxSize()
                    // Prevent accidental routing via spamming of buttons by disabling all input while transitioning out
                    .disablePointerInput(transition.targetState == EnterExitState.PostExit)
                ) {
                    content(state.route)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun createRouteTransition(): AnimatedContentScope<RouteStackEntry>.() -> ContentTransform {
    val density = LocalDensity.current

    return {
        val isPopping = initialState.lifecycle == RouteLifecycle.Destroyed
        val route = if (isPopping) initialState.route else targetState.route

        if (route is IAnimatedRoute) {
            (if (isPopping) route.transition.popEnter else route.transition.enter)(density) with
                    (if (isPopping) route.transition.popExit else route.transition.exit)(density)
        } else {
            (if (isPopping) DefaultRouteTransition.popEnter else DefaultRouteTransition.enter)(density) with
                    (if (isPopping) DefaultRouteTransition.popExit else DefaultRouteTransition.exit)(density)
        }
    }
}

val LocalRouter = staticCompositionLocalOf<IRouter> { Router(object : IRoute {}) }
val LocalRoute = staticCompositionLocalOf { RouteStackEntry(object : IRoute {}) }
val LocalRouteScope = staticCompositionLocalOf { createRouteScope() }

/**
 * [RouteDestroyedEffect] runs only once when the [IRoute] is popped off the backstack. If the route the effect is
 * attached to is currently visible in the composition, the effect will not be run until the composable has been disposed
 */
@Composable
@NonRestartableComposable
fun RouteDestroyedEffect(effect: () -> Unit) {
    val router = LocalRouter.current
    val route = LocalRoute.current

    return DisposableEffect(Unit) {
        if (router is Router) router.addRouteDestroyedListener(route, effect)

        onDispose {
            if (route.lifecycle == RouteLifecycle.Destroyed) effect()
        }
    }
}