package com.outsidesource.oskitkmp.router

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.collections.plus

/**
 * [Router] the primary implementation of IRouter
 */
class Router(
    initialRoute: IRoute,
    private val defaultTransition: IRouteTransition = DefaultTransition,
) : IRouter {
    override val routeFlow: MutableStateFlow<RouteStackEntry>
    override val current get() = _routeStack.value.last()
    override val routeStack: List<RouteStackEntry> get() = _routeStack.value

    private val _routeStack: AtomicRef<List<RouteStackEntry>>
    private val routeLifecycleListeners = atomic(mapOf<Int, List<IRouteLifecycleListener>>())
    private var transitionStatus: RouteTransitionStatus by atomic(RouteTransitionStatus.Idle)
    private val onRouteDestroyedTransitionCompletedCallbacks = atomic<List<() -> Unit>>(emptyList())

    init {
        val initialStackEntry = RouteStackEntry(initialRoute)
        _routeStack = atomic(listOf(initialStackEntry))
        routeFlow = MutableStateFlow(initialStackEntry)
        initForPlatform(this)
    }

    override fun push(route: IRoute, transition: IRouteTransition?, ignoreTransitionLock: Boolean) =
        transaction(ignoreTransitionLock) { push(route, transition) }

    override fun replace(route: IRoute, transition: IRouteTransition?, ignoreTransitionLock: Boolean) =
        transaction(ignoreTransitionLock) { replace(route, transition) }

    internal fun push(route: RouteStackEntry) =
        transaction { (this as? RouterTransactionScope)?.push(route) }

    override fun pop(ignoreTransitionLock: Boolean, popFunc: RoutePopFunc) =
        transaction(ignoreTransitionLock) { pop(popFunc) }

    override fun transaction(ignoreTransitionLock: Boolean, transaction: IRouterTransactionScope.() -> Unit) {
        if (transitionStatus == RouteTransitionStatus.Running && !ignoreTransitionLock) return

        val scope = RouterTransactionScope(
            routeStack = routeStack.toList(),
            defaultTransition = defaultTransition,
            onRouteStopped = ::onRouteStopped,
            onRouteDestroyed = ::onRouteDestroyed,
        ).apply { transaction() }
        _routeStack.value = scope.routeStack

        val top = _routeStack.value.last()
        routeFlow.value = top
        routeLifecycleListeners.value[top.id]?.forEach { it.onRouteStarted() }
    }

    override fun hasBackStack(): Boolean = _routeStack.value.size > 1

    override fun markTransitionStatus(status: RouteTransitionStatus) {
        val previousStatus = transitionStatus
        transitionStatus = status

        if (status == RouteTransitionStatus.Idle && previousStatus == RouteTransitionStatus.Running) {
            val routeDestroyedTransitionCompleteCallbacks = onRouteDestroyedTransitionCompletedCallbacks.value
            onRouteDestroyedTransitionCompletedCallbacks.value = emptyList()

            for (callback in routeDestroyedTransitionCompleteCallbacks) {
                callback()
            }

            val top = _routeStack.value.last()
            routeLifecycleListeners.value[top.id]?.forEach { it.onRouteEnterTransitionComplete() }
        }
    }

    override fun addRouteLifecycleListener(listener: IRouteLifecycleListener) {
        val route = current
        listener.onRouteCreated()
        listener.onRouteStarted()
        routeLifecycleListeners.update {
            it.toMutableMap().apply {
                put(route.id, (this[route.id] ?: emptyList()) + listener)
            }
        }
    }

    override fun tearDown() = tearDownForPlatform(this)

    private fun onRouteStopped(route: RouteStackEntry) {
        routeLifecycleListeners.value[route.id]?.forEach { listener -> listener.onRouteStopped() }
    }

    private fun onRouteDestroyed(route: RouteStackEntry) {
        routeLifecycleListeners.value[route.id]?.forEach { listener ->
            onRouteDestroyedTransitionCompletedCallbacks.update { it + listener::onRouteDestroyedTransitionComplete }
            listener.onRouteDestroyed()
        }
        routeLifecycleListeners.update { it.toMutableMap().apply { remove(route.id) } }
    }
}

private class RouterTransactionScope(
    var routeStack: List<RouteStackEntry>,
    val defaultTransition: IRouteTransition,
    val onRouteStopped: (RouteStackEntry) -> Unit,
    val onRouteDestroyed: (RouteStackEntry) -> Unit,
) : IRouterTransactionScope {

    private val popScope = object : IRoutePopScope {}
    private var hasStoppedTop = false

    override fun replace(route: IRoute, transition: IRouteTransition?) {
        destroyTopStackEntry()
        push(route, transition)
    }

    override fun push(route: IRoute, transition: IRouteTransition?) {
        stopTopRoute(routeStack.lastOrNull())
        val entry = RouteStackEntry(
            route = route,
            transition = transition ?: if (route is IAnimatedRoute) route.transition else defaultTransition,
        )
        routeStack += entry
    }

    override fun pop(block: RoutePopFunc) {
        val popFunc = popScope.block()
        while (routeStack.size > 1) {
            if (!popFunc(routeStack.last().route)) return
            destroyTopStackEntry()
        }
    }

    fun push(entry: RouteStackEntry) {
        stopTopRoute(routeStack.last())
        routeStack += entry
    }

    private fun destroyTopStackEntry() {
        val top = routeStack.last()
        routeStack -= top
        stopTopRoute(top)
        onRouteDestroyed(top)
    }

    private fun stopTopRoute(route: RouteStackEntry?) {
        if (route == null) return
        if (hasStoppedTop) return
        hasStoppedTop = true
        onRouteStopped(route)
    }
}

internal expect fun initForPlatform(router: Router)
internal expect fun tearDownForPlatform(router: Router)

// Kotlin 2.1.0 has an issue with an anonymous object being created in a class constructor on iOS preventing compilation
// of any project using OSKit-KMP
internal val DefaultTransition = object : IRouteTransition {}
