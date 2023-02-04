package com.outsidesource.oskitkmp.router

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity

internal val localRouteObjectStore = staticCompositionLocalOf { RouteObjectStore() }
internal val localRouter = staticCompositionLocalOf<IRouter> { Router(object : IRoute {}) }
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
 * [effectId] Uniquely identifies the effect for a given route. [effectId] should be a unique constant.
 */
@Composable
@NonRestartableComposable
@Suppress("UNCHECKED_CAST")
fun RouteDestroyedEffect(effectId: String, effect: () -> Unit) {
    val router = localRouter.current

    val (storedEffect, isDestroyedRef) = rememberForRoute(Pair::class.java, effectId) {
        val isDestroyedRef = Ref<Boolean>()
        router.addRouteDestroyedListener { isDestroyedRef.value = true }
        Pair(effect, isDestroyedRef)
    } as Pair<() -> Unit, Ref<Boolean>>

    return DisposableEffect(Unit) {
        onDispose {
            if (isDestroyedRef.value == true) storedEffect()
        }
    }
}

/**
 * [rememberForRoute] Remembers a given object for the lifetime of the route. There may only be one instance of
 * a given class for a given route. Additional instances may be created if a constant and unique [key] is provided.
 */
@Composable
inline fun <reified T : Any> rememberForRoute(key: String? = null, noinline factory: () -> T): T =
    rememberForRoute(T::class.java, key, factory)

/**
 * [rememberForRoute] Remembers a given object for the lifetime of the route. There may only be one instance of
 * a given class for a given route. Additional instances may be created if a constant and unique [key] is provided.
 */
@Composable
@Suppress("UNCHECKED_CAST")
fun <T : Any> rememberForRoute(objectType: Class<T>, key: String? = null, factory: () -> T): T {
    val objectStore = localRouteObjectStore.current
    val route = LocalRoute.current
    val router = localRouter.current

    val storedObject = objectStore[route.id, key, objectType]

    return if (storedObject != null && storedObject::class.java == objectType) storedObject as T else factory().apply {
        objectStore[route.id, key, objectType] = this
        router.addRouteDestroyedListener {
            objectStore.remove(route.id, key, objectType)
        }
    }
}

class RouteObjectStore {
    private val objects = mutableMapOf<String, Any>()

    operator fun <T> get(routeId: Int, key: String?, objectType: Class<T>): Any? {
        return objects["$routeId:${objectType.canonicalName}:${key ?: ""}"]
    }

    operator fun <T> set(routeId: Int, key: String?, objectType: Class<T>, value: T) {
        objects["$routeId:${objectType.canonicalName}:${key ?: ""}"] = value as Any
    }

    fun <T> remove(routeId: Int, key: String?, objectType: Class<T>) {
        objects.remove("$routeId:${objectType.canonicalName}:${key ?: ""}")
    }
}
