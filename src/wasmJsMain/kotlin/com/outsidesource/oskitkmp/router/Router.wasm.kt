package com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.concurrency.Queue
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event
import kotlin.js.Promise

private val wasmRouterScopes = atomic<Map<Router, CoroutineScope>>(emptyMap())

actual fun initForPlatform(router: Router) {
    var ignorePopStates = 0
    var ignorePops = 0
    var routeCache = listOf<RouteStackEntry>()
    var currentIndex = -1
    val queue = Queue()

    val popStateFlow = MutableSharedFlow<PopStateEvent>(extraBufferCapacity = Channel.UNLIMITED)
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    wasmRouterScopes.update { it.toMutableMap().apply { this[router] = scope } }

    val popStateCallback: (Event) -> Unit = { (it as? PopStateEvent)?.let { popStateFlow.tryEmit(it) } }
    window.addEventListener("popstate", popStateCallback)
    popStateFlow.launchIn(scope).invokeOnCompletion { window.removeEventListener("popstate", popStateCallback) }

    // Listen for route changes
    router.routerListener = object : IRouterListener {

        override fun onPush(newTop: RouteStackEntry) = queue.enqueue {
            if (newTop.route !is IWebRoute) return@enqueue
            val path = newTop.route.webRoutePath ?: return@enqueue

            routeCache = routeCache.subList(0, currentIndex + 1) + newTop
            currentIndex = routeCache.size - 1

            println("Pushing - ${newTop.id}")
            window.history.pushState(newTop.id.toJsNumber(), newTop.route.webRouteTitle ?: "", path)
        }

        override fun onReplace(newTop: RouteStackEntry) = queue.enqueue {
            if (newTop.route !is IWebRoute) return@enqueue
            val path = newTop.route.webRoutePath ?: return@enqueue

            routeCache = routeCache.mapIndexed { i, cacheEntry -> if (i == currentIndex) newTop else cacheEntry }
            println(routeCache.joinToString(", ") { "${it.id} - ${it.route}" })

            window.history.replaceState(newTop.id.toJsNumber(), newTop.route.webRouteTitle ?: "", path)
            println("Replacing - ${window.history.state} - $path")
        }

        override fun onPop(newTop: RouteStackEntry) = queue.enqueue {
            if (ignorePops > 0) {
                ignorePops--
                return@enqueue
            }

            if (newTop.route !is IWebRoute) return@enqueue
            val newIndex = routeCache.indexOfFirst { it.id == newTop.id }
            ignorePopStates++
            println("Popping - ${newTop.id}")
            window.history.go(newIndex - currentIndex)
            // history.go() is asynchronous, so we need to await a popState to know when it's finished
            awaitNavigation().await<JsAny?>()
            currentIndex = newIndex
        }
    }

    router.routerListener?.onPush(router.current)

    // Listen for pops
    popStateFlow.onEach { ev ->
        println("PopState state - ${ev.state}")
        if (ignorePopStates > 0) {
            ignorePopStates--
            return@onEach
        }
        currentIndex = routeCache.indexOfFirst { it.id == router.current.id }
        val newIndex = routeCache.indexOfFirst { it.id == (ev.state as? JsNumber?)?.toInt() }

        println("PopState - $currentIndex - $newIndex - ${routeCache[newIndex]} - State: ${window.history.state}")

        when {
            newIndex == -1 -> {
                // TODO: I think this causes problems
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

private fun awaitNavigation(): Promise<JsAny?> = js(
    """(
    new Promise((res, rej) => {
        const callback = (ev) => {
            window.removeEventListener("popstate", callback)
            res()
        }
        window.addEventListener("popstate", callback)
    })   
    )""",
)

actual fun tearDownForPlatform(router: Router) {
    val scope = wasmRouterScopes.value[router] ?: return
    scope.coroutineContext.cancelChildren()
    wasmRouterScopes.update { it.toMutableMap().apply { remove(router) } }
}
