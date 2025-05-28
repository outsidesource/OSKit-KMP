package com.outsidesource.oskitkmp.coordinator

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.router.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * An abstraction of [Router] to protect against direct utilization of router methods. Coordinators act as the
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
        ignoreTransitionLock: Boolean = false,
    ) = router.push(route, transition, ignoreTransitionLock)

    protected fun replace(
        route: IRoute,
        transition: IRouteTransition? = null,
        ignoreTransitionLock: Boolean = false,
    ) = router.replace(route, transition, ignoreTransitionLock)

    protected fun pop(
        ignoreTransitionLock: Boolean = false,
        popFunc: RoutePopFunc = { once() },
    ) = router.pop(ignoreTransitionLock, popFunc)

    protected fun transaction(
        ignoreTransitionLock: Boolean = false,
        transaction: IRouterTransactionScope.() -> Unit,
    ) = router.transaction(ignoreTransitionLock, transaction)

    suspend fun <T : Any> transactionWithResult(
        resultType: KClass<T>,
        ignoreTransitionLock: Boolean = false,
        transaction: IRouterTransactionScope.() -> Unit,
    ): Outcome<T, RouteResultError> = router.transactionWithResult(resultType, ignoreTransitionLock, transaction)

    fun addRouteLifecycleListener(listener: IRouteLifecycleListener) = router.addRouteLifecycleListener(listener)

    companion object {
        fun createObserver(coordinator: Coordinator): ICoordinatorObserver =
            object : ICoordinatorObserver {
                override fun hasBackStack() = coordinator.router.hasBackStack()

                override fun pop(ignoreTransitionLock: Boolean) = coordinator.router.pop(ignoreTransitionLock)

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
 * Used in the UI layer for observing and reacting to route changes without exposing route stack management.
 */
interface ICoordinatorObserver {
    fun hasBackStack(): Boolean
    fun pop(ignoreTransitionLock: Boolean = false)
    val routeFlow: StateFlow<RouteStackEntry>
    val routeStack: List<RouteStackEntry>
    fun markTransitionStatus(status: RouteTransitionStatus)
    fun addRouteLifecycleListener(listener: IRouteLifecycleListener)
}
