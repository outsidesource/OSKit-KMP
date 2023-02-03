package com.outsidesource.oskitkmp.coordinator

import com.outsidesource.oskitkmp.router.IRoute
import com.outsidesource.oskitkmp.router.IRouteTransition
import com.outsidesource.oskitkmp.router.Router
import kotlin.reflect.KClass

abstract class Coordinator(
    initialRoute: IRoute,
    defaultTransition: IRouteTransition = object : IRouteTransition {}
) {
    internal val router = Router(initialRoute, defaultTransition)

    protected val current = router.current

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
