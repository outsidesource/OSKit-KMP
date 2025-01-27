package com.outsidesource.oskitkmp.router

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

private var routeUid: AtomicInt = atomic(0)
private fun uid(): Int = routeUid.incrementAndGet()

/**
 * [IRouteTransition] the empty interface that defines a route transition. An IRouteTransition may define a transition
 * however the UI needs.
 */
interface IRouteTransition

/**
 * [IRoute] the empty interface that defines a route. This must be unique and implement equals(). This is normally
 * a data class.
 */
interface IRoute

/**
 * [IWebRoute] defines a route that supports Web Browsers and allows a router to update the title and URL bar.
 * Note: When using [IWebRoute] with Compose Multiplatform your application and web server must be configured to handle
 * paths via [configureWebResources.resourcePathMapping] and potentially web server path rewrites to avoid 404s and errors
 */
interface IWebRoute : IRoute {
    val title: String?
    val path: String?
}

/**
 * [IAnimatedRoute] defines an animated route.
 */
interface IAnimatedRoute : IRoute {
    val transition: IRouteTransition
}

/**
 * [RouteTransitionStatus] defines if a transition is running
 */
enum class RouteTransitionStatus {
    Running,
    Completed,
}

/**
 * [RouteStackEntry] defines an entry in the route stack. It contains a unique id for the entry, the [IRoute] that
 * identifies the route, and its lifecycle
 */
data class RouteStackEntry(
    val route: IRoute,
    val transition: IRouteTransition? = null,
    val id: Int = uid(),
) {
    override fun equals(other: Any?): Boolean = if (other is RouteStackEntry) id == other.id else false
    override fun hashCode(): Int = id
}

/**
 * [IRouter] the public navigation interface for use in composables
 */
interface IRouter {
    val routeStack: List<RouteStackEntry>
    val current: RouteStackEntry
    val routeFlow: StateFlow<RouteStackEntry>

    /**
     * TODO: documentation
     */
    fun transaction(force: Boolean = false, block: IRouterTransactionScope.() -> Unit)

    /**
     * [push] navigates to the given route and moves the current active route to an inactive state
     * [transition] defines the route transition
     * [force] indicates if [push] should ignore the current transition status
     */
    fun push(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false)

    /**
     * TODO: Documentation
     */
    fun replace(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false)

    /**
     * TODO: documentation
     */
    fun pop(force: Boolean = false, block: RoutePopFunc = { once() })

    /**
     * [hasBackStack] returns `true` if there is a route to pop off of the route stack
     */
    fun hasBackStack(): Boolean

    /**
     * [markTransitionStatus] allows the router to block spamming of push/pop operations if a transition is ongoing
     */
    fun markTransitionStatus(status: RouteTransitionStatus)

    /**
     * [addRouteLifecycleListener] adds a lifecycle listener to the current route. onRouteStarted() is called immediately.
     * The listener is automatically removed when the route is destroyed.
     */
    fun addRouteLifecycleListener(listener: IRouteLifecycleListener)
}

interface IRouterTransactionScope {
    fun push(route: IRoute, transition: IRouteTransition? = null)
    fun replace(route: IRoute, transition: IRouteTransition? = null)
    fun pop(block: RoutePopFunc = { once() })
}

typealias RoutePopFunc = IRoutePopScope.() -> (route: IRoute) -> Boolean

interface IRoutePopScope {
    fun once(): (route: IRoute) -> Boolean {
        var breakNext = false

        return fun(_: IRoute): Boolean {
            if (!breakNext) {
                breakNext = true
                return true
            }
            return false
        }
    }

    fun <T : IRoute> toClass(to: KClass<T>, inclusive: Boolean = false): (route: IRoute) -> Boolean {
        var breakNext = false

        return fun (route: IRoute): Boolean {
            if (breakNext) {
                return false
            } else if (route::class == to) {
                if (!inclusive) return false
                breakNext = true
            }
            return true
        }
    }

    fun toRoute(to: IRoute, inclusive: Boolean = false): (route: IRoute) -> Boolean {
        var breakNext = false

        return fun (route: IRoute): Boolean {
            if (breakNext) {
                return false
            } else if (route == to) {
                if (!inclusive) return false
                breakNext = true
            }

            return true
        }
    }

    fun whileTrue(block: (IRoute) -> Boolean): (route: IRoute) -> Boolean = block
}

/**
 * [IRouteLifecycleListener]
 */
interface IRouteLifecycleListener {
    /**
     * [onRouteCreated] called immediately after adding a listener
     */
    fun onRouteCreated() {}

    /**
     * [onRouteStarted] called immediately after adding a listener and when a route returns to the foreground after being
     * in the back stack
     */
    fun onRouteStarted() {}

    /**
     * [onRouteStopped] called when the route is placed in the back stack or is about to be destroyed
     */
    fun onRouteStopped() {}

    /**
     * [onRouteDestroyed] called when the route is popped off the stack
     */
    fun onRouteDestroyed() {}

    /**
     * [onRouteDestroyedTransitionComplete] called when the route is destroyed and the exit transition is complete
     */
    fun onRouteDestroyedTransitionComplete() {}

    /**
     * [onRouteEnterTransitionComplete] called when the route's enter transition is complete
     */
    fun onRouteEnterTransitionComplete() {}
}
