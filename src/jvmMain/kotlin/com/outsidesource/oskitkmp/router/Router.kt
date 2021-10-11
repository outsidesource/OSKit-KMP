package com.outsidesource.oskitkmp.router

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass

private var routeUid: Int = 0

private fun uid(): Int = routeUid++

/**
 * [IRoute] the empty interface that defines a route. This must be unique and implement equals(). This is normally
 * a data class.
 */
interface IRoute

/**
 * [IRouter] the public navigation interface for use in composables
 */
interface IRouter {
    fun push(route: IRoute)
    fun replace(route: IRoute)
    fun pop()
    fun popWhile(block: (entry: IRoute) -> Boolean)
    fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean)
    fun hasBackStack(): Boolean
}

/**
 * [RouteLifecycle] defines the current status of a route on the route stack.
 */
enum class RouteLifecycle {
    Active, // Visible in the current composition
    Inactive, // Not visible in the current composition
    Destroyed, // Popped off the route stack
}

enum class RouteViewStatus {
    Visible, // View is visible in current composition
    Disposed, // View was disposed in composition
}

/**
 * [RouteStackEntry] defines an entry in the route stack. It contains a unique id for the entry, the [IRoute] that
 * identifies the route, and its lifecycle
 */
data class RouteStackEntry(
    val route: IRoute,
    var lifecycle: RouteLifecycle = RouteLifecycle.Active,
    val id: Int = uid(),
)

/**
 * [RouteChangeListener] defines a listener for any route changes in the router
 */
typealias RouteChangeListener = (route: RouteStackEntry) -> Unit

/**
 * [Router] the primary routing framework. [Router] is **not** thread-safe and should only be manipulated from the main
 * thread.
 */
class Router(initialRoute: IRoute) : IRouter {
    private val routeStack: MutableList<RouteStackEntry> = mutableListOf(RouteStackEntry(initialRoute))
    private val listeners: MutableList<RouteChangeListener> = mutableListOf()
    private val routeDestroyedListeners: MutableMap<Int, MutableList<() -> Unit>> = mutableMapOf()
    private val routeViewStatus: MutableMap<Int, RouteViewStatus> = mutableMapOf()
    val current get() = routeStack.last()

    /**
     * [subscribe] adds a [RouteChangeListener] subscription to the router to be notified of any route changes
     */
    fun subscribe(listener: RouteChangeListener) = listeners.add(listener)

    /**
     * [unsubscribe] removes the given [RouteChangeListener] from the router
     */
    fun unsubscribe(listener: RouteChangeListener) = listeners.remove(listener)

    /**
     * [push] navigates to the given route and moves the current active route to an inactive state
     */
    override fun push(route: IRoute) {
        val entry = RouteStackEntry(route)
        routeStack.last().lifecycle = RouteLifecycle.Inactive
        routeStack += entry
        notifyListeners()
    }

    /**
     * [replace] replaces the current active route with the provided route
     */
    override fun replace(route: IRoute) {
        val entry = RouteStackEntry(route)
        destroyTopStackEntry()
        routeStack += entry
        notifyListeners()
    }

    /**
     * [pop] pops the current active route off of the route stack and destroys it
     */
    override fun pop() {
        if (routeStack.size <= 1) return
        destroyTopStackEntry()
        routeStack.last().lifecycle = RouteLifecycle.Active
        notifyListeners()
    }

    /**
     * [popTo] pops to a specific [IRoute] in the route stack. If the route does not exist, the router will pop back
     * to the initial route. Setting [inclusive] to `true` will also pop the passed in [to] parameter off of the state.
     * [inclusive] default is `false`
     */
    override fun <T : IRoute> popTo(to: KClass<T>, inclusive: Boolean) {
        if (routeStack.size <= 1) return
        destroyTopStackEntry()

        while (routeStack.size > 1) {
            val top = routeStack.last()
            if (top.route::class == to) {
                if (!inclusive) break
                destroyTopStackEntry()
                break
            }

            destroyTopStackEntry()
        }
        routeStack.last().lifecycle = RouteLifecycle.Active
        notifyListeners()
    }

    /**
     * [popWhile] pops while the passed in [block] returns true
     */
    override fun popWhile(block: (route: IRoute) -> Boolean) {
        if (routeStack.size <= 1) return
        destroyTopStackEntry()

        while (routeStack.size > 1) {
            if (!block(routeStack.last().route)) break
            destroyTopStackEntry()
        }

        routeStack.last().lifecycle = RouteLifecycle.Active
        notifyListeners()
    }

    /**
     * [hasBackStack] returns `true` if there is a route to pop off of the route stack
     */
    override fun hasBackStack(): Boolean = routeStack.size > 1

    /**
     * [setRouteViewStatus] is used to help track if a composable is active or disposed. This allows the router to
     * accurately run `RouteDestroyedEffect` in circumstances where `pop` is called multiple times immediately.
     * **This function should only be used if you are implementing your own [RouteSwitch]**
     */
    fun setRouteViewStatus(entry: RouteStackEntry, status: RouteViewStatus) = routeViewStatus.put(entry.id, status)

    internal fun addRouteDestroyedListener(entry: RouteStackEntry, block: () -> Unit) {
        routeDestroyedListeners.putIfAbsent(entry.id, mutableListOf())
        routeDestroyedListeners[entry.id]?.add(block)
    }

    private fun destroyTopStackEntry() {
        val top = routeStack.removeLast()
        if (routeViewStatus[top.id] == RouteViewStatus.Disposed) routeDestroyedListeners[top.id]?.forEach { it() }
        routeViewStatus.remove(top.id)
        routeDestroyedListeners.remove(top.id)
        top.lifecycle = RouteLifecycle.Destroyed
    }

    private fun notifyListeners() = listeners.forEach { it(routeStack.last()) }
}

internal fun createRouteScope() = CoroutineScope(Dispatchers.Default + SupervisorJob())