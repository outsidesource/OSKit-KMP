package com.outsidesource.oskitkmp.coordinator

import com.outsidesource.oskitkmp.router.*
import kotlinx.coroutines.flow.StateFlow

/**
 * An abstraction around [Router] to protect against direct utilization of router methods. Coordinators act as the
 * mediator between interactors and a router.
 */
abstract class Coordinator(
    initialRoute: IRoute,
    defaultTransition: IRouteTransition = DefaultTransition,
) {
    internal val router = Router(initialRoute, defaultTransition)

    protected val routeStack get() = router.routeStack
    protected val current get() = router.current

    protected fun hasBackStack() = router.hasBackStack()

    protected fun push(
        route: IRoute,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    ) = router.push(route, transition, force)

    protected fun replace(
        route: IRoute,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    ) = router.replace(route, transition, force)

    protected fun pop(
        force: Boolean = false,
        block: RoutePopFunc = { once() },
    ) = router.pop(force, block)

    protected fun transaction(
        force: Boolean = false,
        block: IRouterTransactionScope.() -> Unit,
    ) = router.transaction(force, block)

    fun addRouteLifecycleListener(listener: IRouteLifecycleListener) = router.addRouteLifecycleListener(listener)

    companion object {
        fun createObserver(coordinator: Coordinator): ICoordinatorObserver =
            object : ICoordinatorObserver {
                override fun hasBackStack() = coordinator.router.hasBackStack()

                override fun pop() = coordinator.router.pop()

                override val routeFlow: StateFlow<RouteStackEntry> = coordinator.router.routeFlow
                override val routeStack: List<RouteStackEntry>
                    get() = coordinator.router.routeStack

                override fun markTransitionStatus(status: RouteTransitionStatus) =
                    coordinator.router.markTransitionStatus(status)

                override fun addRouteLifecycleListener(listener: IRouteLifecycleListener) =
                    coordinator.router.addRouteLifecycleListener(listener)
            }
    }
}

/**
 * [ICoordinatorObserver]'s main purpose is to be used in the UI layer for observing and reacting to route changes
 * without exposing route stack management.
 */
interface ICoordinatorObserver {
    fun hasBackStack(): Boolean
    fun pop()
    val routeFlow: StateFlow<RouteStackEntry>
    val routeStack: List<RouteStackEntry>
    fun markTransitionStatus(status: RouteTransitionStatus)
    fun addRouteLifecycleListener(listener: IRouteLifecycleListener)
}
