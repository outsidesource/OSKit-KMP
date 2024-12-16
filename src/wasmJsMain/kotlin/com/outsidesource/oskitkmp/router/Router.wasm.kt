package com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.lib.printAll
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

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

// TODO: Need to allow cleanup of listeners

actual fun initForPlatform(router: Router) {
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

            printAll("handling new route")

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

            printAll("Stack", router.routeStack.joinToString(", ") { it.route.toString() })
            printAll("Cache", routeCache.joinToString(", ") { it.route.toString() })
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
            printAll("handling pop state", currentIndex, newIndex)

            handleNewRoute = false
            when {
                newIndex == -1 -> router.popWhile(force = true) { true }
                newIndex < currentIndex -> router.popWhile(force = true) { it != routeCache[newIndex].route }
                newIndex > currentIndex -> for (i in currentIndex + 1..newIndex) { router.push(routeCache[i]) }
            }

            currentIndex = newIndex
            printAll("Stack", router.routeStack.joinToString(", ") { it.route.toString() })
            printAll("Cache", routeCache.joinToString(", ") { it.route.toString() })
        }
    }
}

private fun popStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { (it as? PopStateEvent)?.let { trySend(it) } }
    window.addEventListener("popstate", callback)
    awaitClose { window.removeEventListener("popstate", callback) }
}
