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
     * [push] navigates to the given route and moves the current active route to an inactive state
     * [transition] defines the route transition
     * [force] indicates if [push] should ignore the current transition status
     */
    fun push(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false)
    fun push(
        route: IRoute,
        transition: IRouteTransition? = null,
        force: Boolean = false,
        popWhile: (entry: IRoute) -> Boolean,
    )

    fun push(
        route: IRoute,
        popTo: IRoute,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    )

    fun <T : IRoute> push(
        route: IRoute,
        popTo: KClass<T>,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    )

    /**
     * [replace] replaces the current active route with the provided route
     * * [transition] defines the route transition
     * [force] indicates if [push] should ignore the current transition status
     */
    fun replace(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false)
    fun replace(
        route: IRoute,
        transition: IRouteTransition? = null,
        force: Boolean = false,
        popWhile: (entry: IRoute) -> Boolean,
    )

    fun replace(
        route: IRoute,
        popTo: IRoute,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    )

    fun <T : IRoute> replace(
        route: IRoute,
        popTo: KClass<T>,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    )

    /**
     * [pop] pops the current active route off of the route stack and destroys it
     */
    fun pop(force: Boolean = false)

    /**
     * [popWhile] pops while the passed in [block] returns true
     * [force] indicates if [push] should ignore the current transition status
     */
    fun popWhile(force: Boolean = false, block: (entry: IRoute) -> Boolean)

    /**
     * [popTo] pops to a specific [IRoute] in the route stack. If the route does not exist, the router will pop back
     * to the initial route. Setting [inclusive] to `true` will also pop the passed in [to] parameter off of the state.
     * [inclusive] default is `false`
     */
    fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean = false, force: Boolean = false)

    /**
     * [popTo] pops to a specific [IRoute] in the route stack. If the route does not exist, the router will pop back
     * to the initial route. Setting [inclusive] to `true` will also pop the passed in [to] parameter off of the state.
     * [inclusive] default is `false`
     * [force] indicates if [push] should ignore the current transition status
     */
    fun popTo(to: IRoute, inclusive: Boolean = false, force: Boolean = false)

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
