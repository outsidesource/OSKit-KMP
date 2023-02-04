package com.outsidesource.oskitkmp.router

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

/**
 * [Router] the primary implementation of IRouter
 */
internal class Router(
    initialRoute: IRoute,
    private val defaultTransition: IRouteTransition = object : IRouteTransition {},
) : IRouter {
    private val _routeStack: AtomicRef<List<RouteStackEntry>>
    private val routeDestroyedListeners = atomic(mapOf<Int, List<() -> Unit>>())
    private var transitionStatus: RouteTransitionStatus by atomic(RouteTransitionStatus.Completed)

    override val routeFlow: MutableStateFlow<RouteStackEntry>
    override val current get() = _routeStack.value.last()
    override val routeStack: List<RouteStackEntry>
        get() = _routeStack.value

    init {
        val initialStackEntry = RouteStackEntry(initialRoute)
        _routeStack = atomic(listOf(initialStackEntry))
        routeFlow = MutableStateFlow(initialStackEntry)
    }

    override fun push(route: IRoute, transition: IRouteTransition?, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running) return
        val entry = RouteStackEntry(
            route = route,
            transition = transition ?: if (route is IAnimatedRoute) route.transition else defaultTransition
        )
        _routeStack.update { it + entry }
        notifyListeners()
    }

    override fun replace(route: IRoute, transition: IRouteTransition?, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running) return
        if (_routeStack.value.last().route == route) return
        val entry = RouteStackEntry(
            route = route,
            transition = if (route is IAnimatedRoute) route.transition else defaultTransition
        )
        destroyTopStackEntry()
        _routeStack.update { it + entry }
        notifyListeners()
    }

    override fun pop(force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running) return
        if (_routeStack.value.size <= 1) return
        destroyTopStackEntry()
        notifyListeners()
    }

    override fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running) return
        var breakNext = false

        popWhile {
            if (breakNext) {
                return@popWhile false
            } else if (it::class == to) {
                if (!inclusive) return@popWhile false
                breakNext = true
            }

            return@popWhile true
        }
    }

    override fun popTo(to: IRoute, inclusive: Boolean, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running) return
        var breakNext = false

        popWhile {
            if (breakNext) {
                return@popWhile false
            } else if (it == to) {
                if (!inclusive) return@popWhile false
                breakNext = true
            }

            return@popWhile true
        }
    }

    override fun popWhile(force: Boolean, block: (route: IRoute) -> Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running) return
        if (_routeStack.value.size <= 1) return
        destroyTopStackEntry()

        while (_routeStack.value.size > 1) {
            if (!block(_routeStack.value.last().route)) break
            destroyTopStackEntry()
        }

        notifyListeners()
    }

    override fun hasBackStack(): Boolean = _routeStack.value.size > 1

    override fun markTransitionStatus(status: RouteTransitionStatus) {
        transitionStatus = status
    }

    private fun destroyTopStackEntry() {
        val top = _routeStack.value.last()
        _routeStack.update { it.toMutableList().apply { remove(top) } }
        routeDestroyedListeners.value[top.id]?.forEach { it() }
        routeDestroyedListeners.update { it.toMutableMap().apply { remove(top.id) } }
    }

    private fun notifyListeners() {
        routeFlow.value = _routeStack.value.last()
    }

    override fun addRouteDestroyedListener(block: () -> Unit) {
        val route = current
        routeDestroyedListeners.update {
            it.toMutableMap().apply {
                put(route.id, (this[route.id] ?: emptyList()) + block)
            }
        }
    }
}
