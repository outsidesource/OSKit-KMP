package com.outsidesource.oskitkmp.router

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

private var routeUid: AtomicInt = atomic(0)

private fun uid(): Int = routeUid.incrementAndGet()

/**
 * [IRoute] the empty interface that defines a route. This must be unique and implement equals(). This is normally
 * a data class.
 */
interface IRoute

/**
 * [IRouteTransition] the empty interface that defines a route transition.
 */
interface IRouteTransition

enum class RouteTransitionStatus {
    Running,
    Completed,
}

/**
 * [IRouter] the public navigation interface for use in composables
 */
interface IRouter {
    fun push(route: IRoute)
    fun replace(route: IRoute)
    fun pop()
    fun popWhile(block: (entry: IRoute) -> Boolean)
    fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean)
    fun popTo(to: IRoute, inclusive: Boolean)
    fun hasBackStack(): Boolean
    fun markTransitionStatus(status: RouteTransitionStatus)
    val routeStack: List<RouteStackEntry>
    val current: RouteStackEntry
    val routeFlow: Flow<RouteStackEntry>
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
 * [Router] the primary routing framework. [Router] is **not** thread-safe and should only be manipulated from the main
 * thread.
 */
class Router(initialRoute: IRoute) : IRouter {
    private val _routeStack: AtomicRef<List<RouteStackEntry>>

    override val routeFlow: MutableStateFlow<RouteStackEntry>
    override val current get() = _routeStack.value.last()
    override val routeStack: List<RouteStackEntry>
        get() = _routeStack.value

    init {
        val initialStackEntry = RouteStackEntry(initialRoute)
        _routeStack = atomic(mutableListOf(initialStackEntry))
        routeFlow = MutableStateFlow(initialStackEntry)
    }

    /**
     * [push] navigates to the given route and moves the current active route to an inactive state
     */
    override fun push(route: IRoute) {
        val entry = RouteStackEntry(route)
        _routeStack.update { it + entry }
        notifyListeners()
    }

    /**
     * [replace] replaces the current active route with the provided route
     */
    override fun replace(route: IRoute) {
        if (_routeStack.value.last().route == route) return
        val entry = RouteStackEntry(route)
        destroyTopStackEntry()
        _routeStack.update { it + entry }
        notifyListeners()
    }

    /**
     * [pop] pops the current active route off of the route stack and destroys it
     */
    override fun pop() {
        if (_routeStack.value.size <= 1) return
        destroyTopStackEntry()
        notifyListeners()
    }

    /**
     * [popTo] pops to a specific [IRoute] in the route stack. If the route does not exist, the router will pop back
     * to the initial route. Setting [inclusive] to `true` will also pop the passed in [to] parameter off of the state.
     * [inclusive] default is `false`
     */
    override fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean) {
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

    /**
     * [popTo] pops to a specific [IRoute] in the route stack. If the route does not exist, the router will pop back
     * to the initial route. Setting [inclusive] to `true` will also pop the passed in [to] parameter off of the state.
     * [inclusive] default is `false`
     */
    override fun popTo(to: IRoute, inclusive: Boolean) {
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

    /**
     * [popWhile] pops while the passed in [block] returns true
     */
    override fun popWhile(block: (route: IRoute) -> Boolean) {
        if (_routeStack.value.size <= 1) return
        destroyTopStackEntry()

        while (_routeStack.value.size > 1) {
            if (!block(_routeStack.value.last().route)) break
            destroyTopStackEntry()
        }

        notifyListeners()
    }

    /**
     * [hasBackStack] returns `true` if there is a route to pop off of the route stack
     */
    override fun hasBackStack(): Boolean = _routeStack.value.size > 1

    /**
     * [markTransitionStatus] allows the router to block spamming of push/pop operations if a transition is ongoing
     */
    override fun markTransitionStatus(status: RouteTransitionStatus) {
    }

    private fun destroyTopStackEntry() {
        val top = _routeStack.value.last()
        _routeStack.update { it.toMutableList().apply { remove(top) } }
    }

    private fun notifyListeners() {
        routeFlow.value = _routeStack.value.last()
    }
}
