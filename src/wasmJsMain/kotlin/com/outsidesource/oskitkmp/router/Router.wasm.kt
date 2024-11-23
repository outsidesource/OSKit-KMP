package com.outsidesource.oskitkmp.router

import kotlinx.browser.document
import kotlinx.browser.window

actual fun handleNewRouteForPlatform(route: IRoute) {
    if (route !is IWebRoute) return
    route.title?.let { document.title = it }
    route.path?.let { window.history.replaceState(null, route.title ?: "", it) }
}
