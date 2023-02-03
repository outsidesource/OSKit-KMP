package com.outsidesource.oskitkmp.router

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

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
fun RouteSwitch(router: Router, content: @Composable (route: IRoute) -> Unit) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val routeDestroyedEffectHolder = remember { RouterDestroyedEffectHolder() }
    val currentRoute by router.routeFlow.collectAsState()

    BackHandler(enabled = router.hasBackStack()) { router.pop() }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = createRouteTransition()
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun createRouteTransition(): AnimatedContentScope<RouteStackEntry>.() -> ContentTransform {
    val density = LocalDensity.current

    return {
        val isPopping = targetState.id < initialState.id
        val route = if (isPopping) initialState else targetState

        if (route.transition is RouteTransition) {
            (if (isPopping) route.transition.popEnter else route.transition.enter)(density) with
                (if (isPopping) route.transition.popExit else route.transition.exit)(density)
        } else {
            (if (isPopping) DefaultRouteTransition.popEnter else DefaultRouteTransition.enter)(density) with
                (if (isPopping) DefaultRouteTransition.popExit else DefaultRouteTransition.exit)(density)
        }
    }
}

private val localRouter = staticCompositionLocalOf<IRouter> { Router(object : IRoute {}) }
private val localRouteDestroyedEffectHolder = staticCompositionLocalOf { RouterDestroyedEffectHolder() }
val LocalRoute = staticCompositionLocalOf { RouteStackEntry(object : IRoute {}) }

/**
 * [RouteDestroyedEffect] runs only once when the [IRoute] is popped off the backstack. If the route the effect is
 * attached to is currently visible in the composition, the effect will not be run until the composable has been disposed
 *
 * [effectId] Uniquely identifies the effect across for the route. [effectId] should be a unique constant.
 */
@Composable
@NonRestartableComposable
fun RouteDestroyedEffect(effectId: Any, effect: () -> Unit) {
    val destroyedEffectHolder = localRouteDestroyedEffectHolder.current
    val router = localRouter.current
    val route = LocalRoute.current

    return DisposableEffect(Unit) {
        destroyedEffectHolder.add(route.id, effectId, effect)

        onDispose {
            destroyedEffectHolder.invokeEffects(router.routeStack)
        }
    }
}

private class RouterDestroyedEffectHolder {
    private val effectsLock = SynchronizedObject()
    private val effects = mutableMapOf<Int, MutableMap<Any, () -> Unit>>()

    fun add(routeId: Int, effectId: Any, effect: () -> Unit) {
        synchronized(effectsLock) {
            effects[routeId] = (effects[routeId] ?: mutableMapOf()).apply { put(effectId, effect) }
        }
    }

    // Run all RouteDestroyedEffects that are missing from routeStack
    fun invokeEffects(activeRouteStack: List<RouteStackEntry>) {
        val effectMap = synchronized(effectsLock) { effects.toMap() }

        effectMap.keys.sortedDescending().forEach { id ->
            if (activeRouteStack.find { it.id == id } != null) return@forEach
            effectMap[id]?.forEach { (_, v) -> v() }
            remove(id)
        }
    }

    private fun remove(routeId: Int) {
        synchronized(effectsLock) {
            effects.remove(routeId)
        }
    }
}
