package com.outsidesource.oskitkmp.router

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event

private val wasmRouterScopes = atomic<Map<Router, CoroutineScope>>(emptyMap())

actual fun initForPlatform(router: Router) {
    var ignorePopStates = 0
    var ignorePops = 0
    var routeCache = listOf<RouteStackEntry>()
    var currentIndex = -1

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    wasmRouterScopes.update { it.toMutableMap().apply { this[router] = scope } }

    // Listen for route changes
    router.routerListener = object : IRouterListener {
        override fun onPush(newTop: RouteStackEntry) {
            if (newTop.route !is IWebRoute) return
            val path = newTop.route.webRoutePath ?: return

            routeCache = routeCache.subList(0, currentIndex + 1) + newTop
            currentIndex = routeCache.size - 1

            window.history.pushState(newTop.id.toJsNumber(), newTop.route.webRouteTitle ?: "", path)
        }

        override fun onReplace(newTop: RouteStackEntry) {
            if (newTop.route !is IWebRoute) return
            val path = newTop.route.webRoutePath ?: return

            routeCache = routeCache.mapIndexed { i, cacheEntry -> if (i == currentIndex) newTop else cacheEntry }

            window.history.replaceState(newTop.id.toJsNumber(), newTop.route.webRouteTitle ?: "", path)
        }

        override fun onPop(newTop: RouteStackEntry) {
            if (ignorePops > 0) {
                ignorePops--
                return
            }

            if (newTop.route !is IWebRoute) return
            val newIndex = routeCache.indexOfFirst { it.id == newTop.id }
            ignorePopStates++
            window.history.go(newIndex - currentIndex)
            currentIndex = newIndex
        }
    }

    router.routerListener?.onPush(router.current)

    // Listen for pops
    popStateFlow().onEach { ev ->
        if (ignorePopStates > 0) {
            ignorePopStates--
            return@onEach
        }
        currentIndex = routeCache.indexOfFirst { it.id == router.current.id }
        val newIndex = routeCache.indexOfFirst { it.id == (ev.state as? JsNumber?)?.toInt() }

        when {
            newIndex == -1 -> {
                router.pop(ignoreTransitionLock = true) {
                    whileTrue {
                        ignorePops++
                        true
                    }
                }
            }
            newIndex < currentIndex -> {
                router.pop(ignoreTransitionLock = true) {
                    whileTrue {
                        // TODO: This might not be accurate. I should really use the entry id
                        val shouldPop = it != routeCache[newIndex].route
                        if (shouldPop) ignorePops++
                        shouldPop
                    }
                }
            }
            newIndex > currentIndex -> {
                for (i in currentIndex + 1..newIndex) { router.push(routeCache[i], ignoreTransitionLock = true) }
            }
        }

        currentIndex = newIndex
    }.launchIn(scope)
}

actual fun tearDownForPlatform(router: Router) {
    val scope = wasmRouterScopes.value[router] ?: return
    scope.coroutineContext.cancelChildren()
    wasmRouterScopes.update { it.toMutableMap().apply { remove(router) } }
}

private fun popStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { (it as? PopStateEvent)?.let { trySend(it) } }
    window.addEventListener("popstate", callback)
    awaitClose { window.removeEventListener("popstate", callback) }
}
