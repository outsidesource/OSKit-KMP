package com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

private var routeUid: AtomicInt = atomic(0)
private fun uid(): Int = routeUid.incrementAndGet()

/**
 * Defines a route. This must be unique and implement equals(). This is normally a data class.
 */
interface IRoute

/**
 * Defines a route that supports Web Browsers and allows a router to update the title and URL bar.
 * Note: When using [IWebRoute] with Compose Multiplatform your application and web server must be configured to handle
 * paths via [configureWebResources.resourcePathMapping] and potentially web server path rewrites to avoid 404s and errors
 */
interface IWebRoute : IRoute {
    val title: String?
    val path: String?
}

/**
 * Defines an animated route.
 */
interface IAnimatedRoute : IRoute {
    val transition: IRouteTransition
}

/**
 * Defines a route transition. An IRouteTransition may define a transition however the UI needs.
 */
interface IRouteTransition

/**
 * Defines if a transition between routes is running or Idle
 */
enum class RouteTransitionStatus {
    Running,
    Idle,
}

/**
 * Defines an entry in the route stack. It contains a unique id for the entry, the [IRoute] that
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
 * A Router is stack of [RouteStackEntry] that allows pushing and popping of routes.
 */
interface IRouter {
    val routeStack: List<RouteStackEntry>
    val current: RouteStackEntry
    val routeFlow: StateFlow<RouteStackEntry>

    /**
     * Navigates to the given route and moves the current active route to an inactive state
     *
     * @param transition The transition to use when pushing the new route
     * @param ignoreTransitionLock If true the router will ignore any active transitions and perform the requested
     * action. If false the router will ignore the request if there is an active transition. This prevents button
     * spamming from opening several routes in a short time span.
     */
    fun push(route: IRoute, transition: IRouteTransition? = null, ignoreTransitionLock: Boolean = false)

    /**
     * Navigates to the given route and moves the current active route to an inactive state
     *
     * @param transition The transition to use when pushing the new route
     * @param ignoreTransitionLock If true the router will ignore any active transitions and perform the requested
     * action. If false the router will ignore the request if there is an active transition. This prevents button
     * spamming from opening several routes in a short time span.
     */
    fun replace(route: IRoute, transition: IRouteTransition? = null, ignoreTransitionLock: Boolean = false)

    /**
     * Pops the current active route off of the backstack using the provided [popFunc]
     *
     * @param ignoreTransitionLock If true the router will ignore any active transitions and perform the requested
     * action. If false the router will ignore the request if there is an active transition. This prevents button
     * spamming from opening several routes in a short time span.
     * @param popFunc The function that defines how many and which routes are popped off the stack. See
     * [IRoutePopScope.once], [IRoutePopScope.toRoute], [IRoutePopScope.toClass], [IRoutePopScope.whileTrue] for more
     * information
     *
     * ```
     * router.pop { once() }
     * router.pop { toRoute(Route.Home) }
     * router.pop { whileTrue { it !is Route.Test } }
     * ```
     */
    fun pop(ignoreTransitionLock: Boolean = false, popFunc: RoutePopFunc = { once() })

    /**
     * Allows performing several routing operations in a single transaction. [routeFlow] collectors will only be
     * notified at the end of the transaction a single time.
     *
     * @param ignoreTransitionLock If true the router will ignore any active transitions and perform the requested
     * action. If false the router will ignore the request if there is an active transition. This prevents button
     * spamming from opening several routes in a short time span.
     * @param transaction The transaction to perform
     *
     * ```
     * router.transaction {
     *     pop { whileTrue { true } }
     *     push(Route.NewRoute1)
     *     push(Route.NewRoute2)
     * }
     * ```
     */
    fun transaction(ignoreTransitionLock: Boolean = false, transaction: IRouterTransactionScope.() -> Unit)

    /**
     * Does everything [IRouter.transaction] does but suspends until a result is set by [IRoutePopScope.withResult] or
     * until the top route is popped off the stack. The last statement in [transactionWithResult] should be a [push] or
     * [replace]
     *
     * ```
     * // HomeScreen.kt
     * val result = router.transactionWithResult(Boolean::class) {
     *      push(Route.NewRoute1)
     * } // Outcome.Ok(true)
     *
     * // NewRoute1Screen.kt
     * pop { withResult(true) }
     * ```
     */
    suspend fun <T : Any> transactionWithResult(
        resultType: KClass<T>,
        ignoreTransitionLock: Boolean = false,
        transaction: IRouterTransactionScope.() -> Unit,
    ): Outcome<T, RouteResultError>

    /**
     * Returns `true` if there is a route to pop off of the route stack
     */
    fun hasBackStack(): Boolean

    /**
     * Allows the router to block spamming of push/pop operations if a transition is ongoing. Implementers of Router
     * should call this at the start and end of any Route transition.
     */
    fun markTransitionStatus(status: RouteTransitionStatus)

    /**
     * Adds a lifecycle listener to the current route. onRouteStarted() is called immediately.
     * The listener is automatically removed when the route is destroyed.
     */
    fun addRouteLifecycleListener(listener: IRouteLifecycleListener)

    /**
     * Allows the router to tear down anything that was initialized when the router was constructed
     */
    fun tearDown()
}

sealed class RouteResultError {
    object Cancelled : RouteResultError()
    data class Unknown(val error: Any) : RouteResultError()
    data class UnexpectedResultType(val result: Any) : RouteResultError()
}

interface IRouterTransactionScope {
    fun push(route: IRoute, transition: IRouteTransition? = null)
    fun replace(route: IRoute, transition: IRouteTransition? = null)
    fun pop(popFunc: RoutePopFunc = { once() })
}

typealias RoutePopFunc = IRoutePopScope.() -> (route: IRoute) -> Boolean

/**
 * The scope all pop operations are performed in
 */
interface IRoutePopScope {

    /**
     * Pops the top route off of the stack unless it is the only route. This is the default operation.
     */
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

    /**
     * Pops to a specified class
     *
     * @param to The class to pop to
     * @param inclusive If true, the route that matches [to] will also be popped unless it is the only route.
     */
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

    /**
     * Pops to a specified Route
     *
     * @param to The route to pop to
     * @param inclusive If true, the route that matches [to] will also be popped unless it is the only route.
     */
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

    /**
     * Pops while a specified condition is true. This is a convenience function for readability.
     *
     * @param predicate The predicate function that determines what routes will be popped. Returning true will pop
     * the route off the stack. Returning false will stop the popping operation.
     */
    fun whileTrue(predicate: (IRoute) -> Boolean): (route: IRoute) -> Boolean = predicate

    /**
     * Pops the top route off of the stack and sets the route's result to the provided [result]. This should be used
     * in conjunction with [IRouter.transactionWithResult]
     */
    fun withResult(result: Any): (route: IRoute) -> Boolean
}

/**
 * [IRouteLifecycleListener]
 */
interface IRouteLifecycleListener {
    /**
     * Called immediately after adding a listener
     */
    fun onRouteCreated() {}

    /**
     * Called immediately after adding a listener and when a route returns to the foreground after being
     * in the back stack
     */
    fun onRouteStarted() {}

    /**
     * Called when the route is placed in the back stack or is about to be destroyed
     */
    fun onRouteStopped() {}

    /**
     * Called when the route is popped off the stack
     */
    fun onRouteDestroyed() {}

    /**
     * Called when the route is destroyed and the exit transition is complete
     */
    fun onRouteDestroyedTransitionComplete() {}

    /**
     * Called when the route's enter transition is complete
     */
    fun onRouteEnterTransitionComplete() {}
}
