package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal val localRouter = staticCompositionLocalOf<IRouter> { Router(object : IRoute {}) }
internal val localRouteDestroyedEffectHolder = staticCompositionLocalOf { RouteDestroyedEffectHolder() }
val LocalRoute = staticCompositionLocalOf { RouteStackEntry(object : IRoute {}) }

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun createComposeRouteTransition(): AnimatedContentScope<RouteStackEntry>.() -> ContentTransform {
    val density = LocalDensity.current

    return {
        val isPopping = targetState.id < initialState.id
        val route = if (isPopping) initialState else targetState
        val transition = if (route.transition is ComposeRouteTransition) route.transition else NoRouteTransition

        (if (isPopping) transition.popEnter else transition.enter)(density) with
            (if (isPopping) transition.popExit else transition.exit)(density)
    }
}

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

internal class RouteDestroyedEffectHolder {
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
