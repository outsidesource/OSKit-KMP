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
    var handlePopState = true
    var handlePop = true
    var routeCache = listOf<RouteStackEntry>()
    var currentIndex = -1

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    wasmRouterScopes.update { it.toMutableMap().apply { this[router] = scope } }

    // Listen for route changes
    router.routerListener = object : IRouterListener {
        override fun onPush(entry: RouteStackEntry) {
            if (entry.route !is IWebRoute) return
            val path = entry.route.webRoutePath ?: return

            routeCache = routeCache.subList(0, currentIndex + 1) + entry
            currentIndex = routeCache.size - 1
            window.history.pushState(entry.id.toJsNumber(), entry.route.webRouteTitle ?: "", path)
        }

        override fun onPop(entry: RouteStackEntry?) {
            if (!handlePop) {
                handlePop = true
                return
            }

            // TODO: Handle replaces properly
            // This only happens when the first route is being replaced
            if (entry == null) {
//                currentIndex = -1
//                routeCache = emptyList()
                return
            }

            if (entry.route !is IWebRoute) return
            val newIndex = routeCache.indexOfFirst { it.id == entry.id }
            handlePopState = false
            window.history.go(newIndex - currentIndex)
            currentIndex = newIndex
        }
    }

    router.routerListener?.onPush(router.current)

    // Listen for pops
    popStateFlow().onEach { ev ->
        if (!handlePopState) {
            handlePopState = true
            return@onEach
        }
        currentIndex = routeCache.indexOfFirst { it.id == router.current.id }
        val newIndex = routeCache.indexOfFirst { it.id == (ev.state as? JsNumber?)?.toInt() }

        when {
            newIndex == -1 -> {
                router.pop(ignoreTransitionLock = true) {
                    whileTrue {
                        handlePop = false
                        true
                    }
                }
            }
            newIndex < currentIndex -> {
                router.pop(ignoreTransitionLock = true) {
                    whileTrue {
                        // TODO: This might not be accurate. I should really use the entry id
                        val shouldPop = it != routeCache[newIndex].route
                        if (shouldPop) handlePop = false
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
