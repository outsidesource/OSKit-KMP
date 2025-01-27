package com.outsidesource.oskitkmp.com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.router.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouterTest {
    @Test
    fun testRouterPush() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.push(Route.Route1)
        router.push(Route.Route1)
        router.push(Route.Route1)
        assertTrue(router.routeStack.size == 5)

        router.transaction {
            pop { toRoute(Route.Home) }
            push(Route.Route2)
        }
        assertTrue(router.routeStack.size == 2 && router.routeStack[0].route == Route.Home)

        router.pop { whileTrue { true } }
        assertEquals(1, router.routeStack.size)
        assertEquals(Route.Home, router.routeStack[0].route)

        router.transaction {
            pop { toRoute(to = Route.Home, inclusive = true) }
            push(Route.Route2)
        }
        assertEquals(2, router.routeStack.size)
        assertEquals(Route.Home, router.routeStack[0].route)
        assertEquals(router.routeFlow.value.route, Route.Route2)
    }

    @Test
    fun testRouterReplace() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.replace(Route.Route2)
        assertEquals(router.routeStack.size, 2)

        router.transaction {
            pop { toRoute(Route.Home) }
            replace(Route.Route2)
        }
        assertEquals(router.routeStack.size, 1)
        assertEquals(router.routeStack[0].route, Route.Route2)
        assertEquals(router.routeFlow.value.route, Route.Route2)
    }

    @Test
    fun testRouterPop() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.push(Route.Route2)
        router.pop()
        assertEquals(router.routeStack.size, 2)
        router.pop()
        assertEquals(router.routeStack.size, 1)
        router.pop()
        assertEquals(router.routeStack.size, 1)
        assertEquals(router.routeFlow.value.route, Route.Home)
    }

    @Test
    fun testRouterPopTo() {
        val router = Router(Route.Home)
        router.push(Route.Route1)
        router.push(Route.Route2)
        router.pop { toClass(Route.Route1::class) }
        assertTrue(router.routeStack.size == 2)
        router.push(Route.Route2)
        router.pop { toClass(Route.Route1::class, inclusive = true) }
        assertTrue(router.routeStack.size == 1)

        router.push(Route.Route1)
        router.push(Route.Route2)
        router.pop { toRoute(Route.Route1) }
        assertEquals(2, router.routeStack.size)
        assertEquals(Route.Route1, router.routeFlow.value.route)
    }

    @Test
    fun testRouterPopWhile() {
        val router = Router(Route.Home)
        router.push(Route.Test(1))
        router.push(Route.Test(2))
        router.push(Route.Test(3))
        router.push(Route.Test(4))
        router.push(Route.Test(5))

        router.pop { whileTrue { it is Route.Test && it.test != 2 } }

        assertEquals(3, router.routeStack.size)
        assertEquals(Route.Test(2), router.routeFlow.value.route)
    }

    @Test
    fun testRouteLifecycle() {
        // TODO
    }
}

private sealed class Route: IRoute {
    data object Home: Route()
    data object Route1: Route()
    data object Route2: Route()
    data class Test(val test: Int): Route()
}