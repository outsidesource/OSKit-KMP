package com.outsidesource.oskitkmp.router

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.PopStateEvent

// TODO: routeCache needs to support multiple routers

val routeCache = mutableListOf<RouteStackEntry>()
var updateState = true

actual fun handleNewRouteForPlatform(router: Router, entry: RouteStackEntry) {
    if (entry.route !is IWebRoute) return
    if (!updateState) return
    entry.route.title?.let { document.title = it }
    entry.route.path?.let { window.history.pushState(entry.id.toJsNumber(), entry.route.title ?: "", it) }
    routeCache += entry
}

actual fun initForPlatform(router: Router) {
    window.addEventListener("popstate") {
        val ev = it as PopStateEvent
        val currentIndex = routeCache.indexOfFirst { it.id == router.current.id }
        val newIndex = routeCache.indexOfFirst { it.id == (ev.state as? JsNumber?)?.toInt() }
        if (newIndex == -1) return@addEventListener

        updateState = false
        if (newIndex < currentIndex) router.popWhile { it != routeCache[newIndex].route }
        if (newIndex > currentIndex) {
            for (i in currentIndex + 1..newIndex) {
                router.push(routeCache[i].route, routeCache[i].transition, true)
            }
        }
        updateState = true
    }
}
