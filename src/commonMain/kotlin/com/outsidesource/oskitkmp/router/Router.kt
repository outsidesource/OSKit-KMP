package com.outsidesource.oskitkmp.router

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

/**
 * [Router] the primary implementation of IRouter
 */
class Router(
    initialRoute: IRoute,
    private val defaultTransition: IRouteTransition = DefaultTransition,
) : IRouter {
    override val routeFlow: MutableStateFlow<RouteStackEntry>
    override val current get() = _routeStack.value.last()
    override val routeStack: List<RouteStackEntry>
        get() = _routeStack.value

    private val _routeStack: AtomicRef<List<RouteStackEntry>>
    private val routeLifecycleListeners = atomic(mapOf<Int, List<IRouteLifecycleListener>>())
    private var transitionStatus: RouteTransitionStatus by atomic(RouteTransitionStatus.Completed)
    private val onRouteDestroyedTransitionCompletedCallbacks = atomic<List<() -> Unit>>(emptyList())

    init {
        val initialStackEntry = RouteStackEntry(initialRoute)
        _routeStack = atomic(listOf(initialStackEntry))
        routeFlow = MutableStateFlow(initialStackEntry)
        initForPlatform(this)
    }

    override fun push(
        route: IRoute,
        popTo: IRoute,
        popToInclusive: Boolean,
        transition: IRouteTransition?,
        force: Boolean,
    ) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        notifyRouteStopped()
        popToInternal(popTo, popToInclusive, force)
        push(route = route, transition = transition, force = force)
    }

    override fun <T : IRoute> push(
        route: IRoute,
        popTo: KClass<T>,
        popToInclusive: Boolean,
        transition: IRouteTransition?,
        force: Boolean,
    ) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        notifyRouteStopped()
        popToInternal(popTo, popToInclusive, force)
        push(route = route, transition = transition, force = force)
    }

    override fun push(route: IRoute, transition: IRouteTransition?, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        notifyRouteStopped()
        pushInternal(route, transition, force)
        notifyRouteFlowListeners()
    }

    override fun push(
        route: IRoute,
        transition: IRouteTransition?,
        force: Boolean,
        popWhile: (entry: IRoute) -> Boolean,
    ) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        notifyRouteStopped()
        popWhileInternal(force, popWhile)
        pushInternal(route, transition, force)
        notifyRouteFlowListeners()
    }

    internal fun push(entry: RouteStackEntry) {
        notifyRouteStopped()
        _routeStack.update { it + entry }
        notifyRouteFlowListeners()
    }

    private fun pushInternal(route: IRoute, transition: IRouteTransition?, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        val entry = RouteStackEntry(
            route = route,
            transition = transition ?: if (route is IAnimatedRoute) route.transition else defaultTransition,
        )
        _routeStack.update { it + entry }
    }

    override fun replace(route: IRoute, transition: IRouteTransition?, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        replaceInternal(route, transition, force)
        notifyRouteFlowListeners()
    }

    override fun replace(
        route: IRoute,
        transition: IRouteTransition?,
        force: Boolean,
        popWhile: (entry: IRoute) -> Boolean,
    ) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        popWhileInternal(force, popWhile)
        replaceInternal(route, transition, force)
        notifyRouteFlowListeners()
    }

    override fun replace(
        route: IRoute,
        popTo: IRoute,
        popToInclusive: Boolean,
        transition: IRouteTransition?,
        force: Boolean,
    ) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        popToInternal(popTo, popToInclusive, force)
        replaceInternal(route, transition, force)
        notifyRouteFlowListeners()
    }

    override fun <T : IRoute> replace(
        route: IRoute,
        popTo: KClass<T>,
        popToInclusive: Boolean,
        transition: IRouteTransition?,
        force: Boolean,
    ) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        popToInternal(popTo, popToInclusive, force)
        replaceInternal(route, transition, force)
        notifyRouteFlowListeners()
    }

    private fun replaceInternal(route: IRoute, transition: IRouteTransition?, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        if (_routeStack.value.last().route == route) return
        val entry = RouteStackEntry(
            route = route,
            transition = transition ?: if (route is IAnimatedRoute) route.transition else defaultTransition,
        )
        destroyTopStackEntry()
        _routeStack.update { it + entry }
    }

    override fun pop(force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        if (_routeStack.value.size <= 1) return
        destroyTopStackEntry()
        notifyRouteFlowListeners()
        notifyRouteStarted()
    }

    override fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        popToInternal(to, inclusive, force)
        notifyRouteFlowListeners()
        notifyRouteStarted()
    }

    override fun popTo(to: IRoute, inclusive: Boolean, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        popToInternal(to, inclusive, force)
        notifyRouteFlowListeners()
        notifyRouteStarted()
    }

    private fun <T : IRoute> popToInternal(to: KClass<T>, inclusive: Boolean, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        var breakNext = false

        popWhileInternal {
            if (breakNext) {
                return@popWhileInternal false
            } else if (it::class == to) {
                if (!inclusive) return@popWhileInternal false
                breakNext = true
            }

            return@popWhileInternal true
        }
    }

    private fun popToInternal(to: IRoute, inclusive: Boolean, force: Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        var breakNext = false

        popWhileInternal {
            if (breakNext) {
                return@popWhileInternal false
            } else if (it == to) {
                if (!inclusive) return@popWhileInternal false
                breakNext = true
            }

            return@popWhileInternal true
        }
    }

    override fun popWhile(force: Boolean, block: (route: IRoute) -> Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        popWhileInternal(force, block)
        notifyRouteFlowListeners()
        notifyRouteStarted()
    }

    private fun popWhileInternal(force: Boolean = false, block: (route: IRoute) -> Boolean) {
        if (transitionStatus == RouteTransitionStatus.Running && !force) return
        if (_routeStack.value.size <= 1) return
        destroyTopStackEntry()

        while (_routeStack.value.size > 1) {
            if (!block(_routeStack.value.last().route)) break
            destroyTopStackEntry(callOnStop = false)
        }
    }

    override fun hasBackStack(): Boolean = _routeStack.value.size > 1

    override fun markTransitionStatus(status: RouteTransitionStatus) {
        val previousStatus = transitionStatus
        transitionStatus = status

        if (status == RouteTransitionStatus.Completed && previousStatus != status) {
            val routeDestroyedCallbacks = onRouteDestroyedTransitionCompletedCallbacks.value
            onRouteDestroyedTransitionCompletedCallbacks.value = emptyList()

            for (callback in routeDestroyedCallbacks) {
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

    private fun destroyTopStackEntry(callOnStop: Boolean = true) {
        val top = _routeStack.value.last()
        _routeStack.update { it.toMutableList().apply { remove(top) } }
        routeLifecycleListeners.value[top.id]?.forEach { listener ->
            if (callOnStop) listener.onRouteStopped()
            onRouteDestroyedTransitionCompletedCallbacks.update { it + listener::onRouteDestroyedTransitionComplete }
            listener.onRouteDestroyed()
        }
        routeLifecycleListeners.update { it.toMutableMap().apply { remove(top.id) } }
    }

    private fun notifyRouteFlowListeners() {
        routeFlow.value = _routeStack.value.last()
    }

    private fun notifyRouteStopped() {
        val top = _routeStack.value.last()
        routeLifecycleListeners.value[top.id]?.forEach { it.onRouteStopped() }
    }

    private fun notifyRouteStarted() {
        val top = _routeStack.value.last()
        routeLifecycleListeners.value[top.id]?.forEach { it.onRouteStarted() }
    }
}

internal expect fun initForPlatform(router: Router)

// Kotlin 2.1.0 has an issue with an anonymous object being created in a class constructor on iOS preventing compilation
// of any project using OSKit-KMP
private val DefaultTransition = object : IRouteTransition {}
