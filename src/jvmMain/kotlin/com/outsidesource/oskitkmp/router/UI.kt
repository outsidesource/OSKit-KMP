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
            LocalRouterProvider provides router,
            LocalRouteProvider provides state,
            LocalRouteScopeProvider provides routeScopeHolder.getOrPut(state.id) { createRouteScope() },
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

val LocalRouterProvider = staticCompositionLocalOf<IRouter> { Router(object : IRoute {}) }
val LocalRouteProvider = staticCompositionLocalOf { RouteStackEntry(object : IRoute {}) }
val LocalRouteScopeProvider = staticCompositionLocalOf { createRouteScope() }

@Composable
fun localRouter(): IRouter = LocalRouterProvider.current

@Composable
fun localRoute(): RouteStackEntry = LocalRouteProvider.current

@Composable
fun localRouteScope(): CoroutineScope = LocalRouteScopeProvider.current

@Composable
@NonRestartableComposable
fun RouteDestroyedEffect(effect: () -> Unit) {
    val router = localRouter()
    val route = localRoute()

    return DisposableEffect(Unit) {
        if (router is Router) router.addRouteDestroyedListener(route, effect)

        onDispose {
            if (route.lifecycle == RouteLifecycle.Destroyed) effect()
        }
    }
}