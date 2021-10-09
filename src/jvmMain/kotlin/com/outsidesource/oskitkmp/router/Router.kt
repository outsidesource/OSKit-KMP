package com.outsidesource.oskitkmp.router

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass

private var routeUid: Int = 0

private fun uid(): Int = routeUid++

interface IRoute

interface IRouter {
    fun push(route: IRoute)
    fun replace(route: IRoute)
    fun pop()
    fun popWhile(block: (entry: IRoute) -> Boolean)
    fun <T: IRoute> popTo(to: KClass<T>, inclusive: Boolean)
    fun hasBackStack(): Boolean
}

enum class RouteLifecycle {
    Active, // Visible in the current composition
    Inactive, // Not visible in the current composition
    Destroyed, // Popped off the route stack
}

data class RouteStackEntry(
    val route: IRoute,
    var lifecycle: RouteLifecycle = RouteLifecycle.Active,
    val id: Int = uid(),
)

typealias RouteChangeListener = (route: RouteStackEntry) -> Unit

class Router(initialRoute: IRoute): IRouter {
    private val routeStack: MutableList<RouteStackEntry> = mutableListOf(RouteStackEntry(initialRoute))
    private val listeners: MutableList<RouteChangeListener> = mutableListOf()
    val current get() = routeStack.last()

    fun subscribe(listener: RouteChangeListener) = listeners.add(listener)

    fun unsubscribe(listener: RouteChangeListener) = listeners.remove(listener)

    override fun push(route: IRoute) {
        val entry = RouteStackEntry(route)
        routeStack.last().lifecycle = RouteLifecycle.Inactive
        routeStack += entry
        notifyListeners()
    }

    override fun replace(route: IRoute) {
        val entry = RouteStackEntry(route)
        routeStack.removeLast().lifecycle = RouteLifecycle.Destroyed
        routeStack += entry
        notifyListeners()
    }

    override fun pop() {
        if (routeStack.size <= 1) return
        routeStack.removeLast().lifecycle = RouteLifecycle.Destroyed
        routeStack.last().lifecycle = RouteLifecycle.Active
        notifyListeners()
    }

    override fun <T: IRoute> popTo(to: KClass<T>, inclusive: Boolean) {
        if (routeStack.size <= 1) return

        while (routeStack.size > 1) {
            val top = routeStack.last()
            if (top.route::class == to) {
                if (!inclusive) break
                routeStack.removeLast().lifecycle = RouteLifecycle.Destroyed
                break
            }
            routeStack.removeLast().lifecycle = RouteLifecycle.Destroyed
        }
        routeStack.last().lifecycle = RouteLifecycle.Active
        notifyListeners()
    }

    override fun popWhile(block: (route: IRoute) -> Boolean) {
        if (routeStack.size <= 1) return
        routeStack.removeLast().lifecycle = RouteLifecycle.Destroyed

        while (routeStack.size > 1) {
            if (!block(routeStack.last().route)) break
            routeStack.removeLast().apply { lifecycle = RouteLifecycle.Destroyed }
        }

        routeStack.last().lifecycle = RouteLifecycle.Active
        notifyListeners()
    }

    override fun hasBackStack(): Boolean = routeStack.size > 1

    private fun notifyListeners() = listeners.forEach { it(routeStack.last()) }
}

internal fun createRouteScope() = CoroutineScope(Dispatchers.Default + SupervisorJob())