package com.outsidesource.oskitkmp.com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.router.IRoute
import com.outsidesource.oskitkmp.router.Router
import kotlin.test.Test

class RouterTest {
    @Test
    fun testRouterPush() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.push(Route.Route1)
        router.push(Route.Route1)
        router.push(Route.Route1)
        assert(router.routeStack.size == 5)

        router.push(route = Route.Route2, popTo = Route.Home)
        assert(router.routeStack.size == 2 && router.routeStack[0].route == Route.Home)

        router.push(route = Route.Route2, popTo = Route.Home, popToInclusive = true)
        // Shouldn't be able to inclusive the Home
        assert(router.routeStack.size == 2 && router.routeStack[0].route == Route.Home)
        assert(router.routeFlow.value.route == Route.Route2)
    }

    @Test
    fun testRouterReplace() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.replace(Route.Route2)
        assert(router.routeStack.size == 2)

        router.replace(route = Route.Route2, popTo = Route.Home)
        assert(router.routeStack.size == 1 && router.routeStack[0].route == Route.Route2)
        assert(router.routeFlow.value.route == Route.Route2)
    }

    @Test
    fun testRouterPop() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.push(Route.Route2)
        router.pop()
        assert(router.routeStack.size == 2)
        router.pop()
        assert(router.routeStack.size == 1)
        router.pop()
        assert(router.routeStack.size == 1)
        assert(router.routeFlow.value.route == Route.Home)
    }

    @Test
    fun testRouterPopTo() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.push(Route.Route2)
        router.popTo(Route.Route1::class)
        assert(router.routeStack.size == 2)
        router.push(Route.Route2)
        router.popTo(Route.Route1::class, inclusive = true)
        assert(router.routeStack.size == 1)

        router.push(Route.Route1)
        router.push(Route.Route2)
        router.popTo(Route.Route1)
        assert(router.routeStack.size == 2)
        assert(router.routeFlow.value.route == Route.Route1)
    }

    @Test
    fun testRouterPopWhile() {
        val router = Router(Route.Home)
        router.push(Route.Test(1))
        router.push(Route.Test(2))
        router.push(Route.Test(3))
        router.push(Route.Test(4))
        router.push(Route.Test(5))

        router.popWhile {
            if (it is Route.Test && it.test != 2) return@popWhile true
            return@popWhile false
        }

        assert(router.routeStack.size == 3)
        assert(router.routeFlow.value.route == Route.Test(2))
    }
}

private sealed class Route: IRoute {
    data object Home: Route()
    data object Route1: Route()
    data object Route2: Route()
    data class Test(val test: Int): Route()
}