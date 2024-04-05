package com.outsidesource.oskitkmp.coordinator

import com.outsidesource.oskitkmp.router.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * An abstraction around [Router] to protect against direct utilization of router methods. Coordinators act as the
 * mediator between interactors and a router.
 */
abstract class Coordinator(
    initialRoute: IRoute,
    defaultTransition: IRouteTransition = object : IRouteTransition {},
) {
    internal val router = Router(initialRoute, defaultTransition)

    protected val routeStack
        get() = router.routeStack
    protected val current
        get() = router.current

    protected fun hasBackStack() = router.hasBackStack()

    protected fun push(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false) =
        router.push(route, transition, force)

    protected fun push(
        route: IRoute,
        popTo: IRoute,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    ) = router.push(route, popTo, popToInclusive, transition, force)

    protected fun <T : IRoute> push(
        route: IRoute,
        popTo: KClass<T>,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    ) = router.push(route, popTo, popToInclusive, transition, force)

    protected fun push(
        route: IRoute,
        transition: IRouteTransition? = null,
        force: Boolean = false,
        popWhile: (entry: IRoute) -> Boolean,
    ) = router.push(route, transition, force, popWhile)

    protected fun replace(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false) =
        router.replace(route, transition, force)

    protected fun replace(
        route: IRoute,
        transition: IRouteTransition? = null,
        force: Boolean = false,
        popWhile: (entry: IRoute) -> Boolean,
    ) = router.replace(route, transition, force, popWhile)

    protected fun replace(
        route: IRoute,
        popTo: IRoute,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    ) = router.replace(route, popTo, popToInclusive, transition, force)

    protected fun <T : IRoute> replace(
        route: IRoute,
        popTo: KClass<T>,
        popToInclusive: Boolean = false,
        transition: IRouteTransition? = null,
        force: Boolean = false,
    ) = router.replace(route, popTo, popToInclusive, transition, force)

    fun pop(force: Boolean = false) = router.pop(force)

    protected fun popWhile(force: Boolean = false, block: (entry: IRoute) -> Boolean) =
        router.popWhile(force, block)

    protected fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean = false, force: Boolean = false) =
        router.popTo(to, inclusive, force)

    protected fun popTo(to: IRoute, inclusive: Boolean = false, force: Boolean = false) =
        router.popTo(to, inclusive, force)

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
