package com.outsidesource.oskitkmp.router

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

// TODO: Need to allow cleanup of listeners

actual fun initForPlatform(router: Router) {
    var updateState = true
    val routeCache = mutableListOf<RouteStackEntry>()

    scope.launch {
        router.routeFlow.collect { entry ->
            if (entry.route !is IWebRoute) return@collect
            if (!updateState) {
                updateState = true
                return@collect
            }
            entry.route.title?.let { document.title = it }
            entry.route.path?.let { window.history.pushState(entry.id.toJsNumber(), entry.route.title ?: "", it) }
            routeCache += entry
        }
    }

    scope.launch {
        popStateFlow().collect { ev ->
            println(ev)
            val currentIndex = routeCache.indexOfFirst { it.id == router.current.id }
            val newIndex = routeCache.indexOfFirst { it.id == (ev.state as? JsNumber?)?.toInt() }
            if (newIndex == -1) return@collect

            updateState = false
            if (newIndex < currentIndex) router.popWhile { it != routeCache[newIndex].route }
            if (newIndex > currentIndex) {
                for (i in currentIndex + 1..newIndex) {
                    router.push(routeCache[i].route, routeCache[i].transition, true)
                }
            }
        }
    }
}

private fun popStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { (it as? PopStateEvent)?.let { trySend(it) } }
    window.addEventListener("popstate", callback)
    awaitClose { window.removeEventListener("popstate", callback) }
}
