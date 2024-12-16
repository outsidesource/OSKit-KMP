package com.outsidesource.oskitkmp.router

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event

actual fun initForPlatform(router: Router) {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var handleNewRoute = true
    var handlePopState = true
    var routeCache = listOf<RouteStackEntry>()
    var previousRouteStack = emptyList<RouteStackEntry>()
    var currentIndex = -1

    scope.launch {
        routeCache = routeCache + router.current

        router.routeFlow.collect { entry ->
            if (entry.route !is IWebRoute) {
                previousRouteStack = router.routeStack
                return@collect
            }
            if (!handleNewRoute) {
                handleNewRoute = true
                previousRouteStack = router.routeStack
                return@collect
            }

            when {
                router.routeStack.size < previousRouteStack.size -> {
                    val newIndex = routeCache.indexOfFirst { it.id == entry.id }
                    handlePopState = false
                    window.history.go(newIndex - currentIndex)
                    currentIndex = newIndex
                }
                else -> {
                    val removeToIndexAdd = if (router.routeStack.size == previousRouteStack.size) 0 else 1
                    routeCache = routeCache.subList(0, currentIndex + removeToIndexAdd) + entry
                    currentIndex = routeCache.size - 1
                    entry.route.title?.let { document.title = it }
                    entry.route.path?.let {
                        if (router.routeStack.size == previousRouteStack.size) {
                            window.history.replaceState(entry.id.toJsNumber(), entry.route.title ?: "", it)
                        } else {
                            window.history.pushState(entry.id.toJsNumber(), entry.route.title ?: "", it)
                        }
                    }
                }
            }

            previousRouteStack = router.routeStack
        }
    }

    scope.launch {
        popStateFlow().collect { ev ->
            if (!handlePopState) {
                handlePopState = true
                return@collect
            }
            currentIndex = routeCache.indexOfFirst { it.id == router.current.id }
            val newIndex = routeCache.indexOfFirst { it.id == (ev.state as? JsNumber?)?.toInt() }

            handleNewRoute = false
            when {
                newIndex == -1 -> router.popWhile(force = true) { true }
                newIndex < currentIndex -> router.popWhile(force = true) { it != routeCache[newIndex].route }
                newIndex > currentIndex -> for (i in currentIndex + 1..newIndex) { router.push(routeCache[i]) }
            }

            currentIndex = newIndex
        }
    }
}

private fun popStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { (it as? PopStateEvent)?.let { trySend(it) } }
    window.addEventListener("popstate", callback)
    awaitClose { window.removeEventListener("popstate", callback) }
}
