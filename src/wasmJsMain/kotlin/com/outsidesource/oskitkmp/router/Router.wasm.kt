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
// TODO: There are some bugs with the interplay of popBackStack and history.back/forward
// TODO: Test replaces

actual fun initForPlatform(router: Router) {
    var handleNewRoute = true
    var handlePopState = true
    val routeCache = mutableListOf<RouteStackEntry>()
    var currentIndex = -1

    scope.launch {
        routeCache += router.current

        router.routeFlow.collect { entry ->
            if (entry.route !is IWebRoute) return@collect
            if (!handleNewRoute) {
                handleNewRoute = true
                return@collect
            }

            val newIndex = routeCache.indexOfFirst { it.id == entry.id }
            printAll("handling route", currentIndex, newIndex)

            when {
                newIndex == -1 || newIndex > currentIndex -> {
                    entry.route.title?.let { document.title = it }
                    entry.route.path?.let {
                        window.history.pushState(entry.id.toJsNumber(), entry.route.title ?: "", it)
                    }
                    routeCache += entry
                    currentIndex = routeCache.size - 1
                }
                newIndex < currentIndex -> {
                    handlePopState = false
                    window.history.go(newIndex - currentIndex)
                    currentIndex = newIndex
                }
            }
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
                newIndex > currentIndex -> for (i in currentIndex + 1..newIndex) {
                    router.push(routeCache[i].route, routeCache[i].transition, force = true)
                }
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
