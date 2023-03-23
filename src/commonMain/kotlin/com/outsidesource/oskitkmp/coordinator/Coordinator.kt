package com.outsidesource.oskitkmp.coordinator

import com.outsidesource.oskitkmp.router.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

abstract class Coordinator(
    initialRoute: IRoute,
    defaultTransition: IRouteTransition = object : IRouteTransition {}
) {
    internal val router = Router(initialRoute, defaultTransition)

    protected val routeStack
        get() = router.routeStack
    protected val current
        get() = router.current
    protected fun hasBackStack() = router.hasBackStack()

    protected fun push(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false) =
        router.push(route, transition, force)

    protected fun replace(route: IRoute, transition: IRouteTransition? = null, force: Boolean = false) =
        router.replace(route, transition, force)

    protected fun pop(force: Boolean = false) = router.pop(force)

    protected fun popWhile(force: Boolean = false, block: (entry: IRoute) -> Boolean) =
        router.popWhile(force, block)

    protected fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean = false, force: Boolean = false) =
        router.popTo(to, inclusive, force)

    protected fun popTo(to: IRoute, inclusive: Boolean = false, force: Boolean = false) =
        router.popTo(to, inclusive, force)
}

fun createCoordinatorObserver(coordinator: Coordinator): ICoordinatorObserver =
    object : ICoordinatorObserver {
        override fun hasBackStack() = coordinator.router.hasBackStack()

        override fun pop() = coordinator.router.pop()

        override val routeFlow: StateFlow<RouteStackEntry> = coordinator.router.routeFlow

        override fun markTransitionStatus(status: RouteTransitionStatus) =
            coordinator.router.markTransitionStatus(status)

        override fun addRouteDestroyedListener(block: () -> Unit) =
            coordinator.router.addRouteDestroyedListener(block)
    }

interface ICoordinatorObserver {
    fun hasBackStack(): Boolean
    fun pop()
    val routeFlow: StateFlow<RouteStackEntry>
    fun markTransitionStatus(status: RouteTransitionStatus)
    fun addRouteDestroyedListener(block: () -> Unit)
}
