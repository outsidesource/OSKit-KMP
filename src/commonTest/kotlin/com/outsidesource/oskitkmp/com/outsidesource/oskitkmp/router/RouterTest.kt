package com.outsidesource.oskitkmp.com.outsidesource.oskitkmp.router

import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import com.outsidesource.oskitkmp.router.*
import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

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
    fun testRouteLifecycle() = runBlockingTest {
        val router = Router(Route.Home)
        val counter = RouteLifecycleCounter()

        router.push(Route.Test(1))
        router.addRouteLifecycleListener(CounterRouteLifecycleListener(counter))

        assertEquals(1, counter.created, "Created not called")
        assertEquals(1, counter.started, "Started not called")

        router.push(Route.Test(2))
        assertEquals(1, counter.stopped, "Stop called wrong number of times")

        router.pop()
        assertEquals(2, counter.started, "Started not called on reentry")

        router.pop()
        val job = launch {
            router.markTransitionStatus(RouteTransitionStatus.Running)
            delay(400)
            router.markTransitionStatus(RouteTransitionStatus.Idle)
        }
        assertEquals(1, counter.destroyed, "Destroyed called wrong number of times")
        job.join()
        assertEquals(1, counter.destroyedTransitionComplete, "Destroyed Transition Complete called wrong number of times")
    }

    @Test
    fun testRouteLifecycle2() {
        val router = Router(Route.Home)
        val counter = RouteLifecycleCounter()

        router.push(Route.Test(1))
        router.addRouteLifecycleListener(CounterRouteLifecycleListener(counter))
        router.push(Route.Test(2))
        router.push(Route.Test(3))
        router.pop { toRoute(Route.Home) }
        assertEquals(1, counter.destroyed, "Destroyed called wrong number of times")
    }

    @Test
    fun testRouteResult() = runBlockingTest {
        val router = Router(Route.Home)

        // Test success
        launch {
            val result = router.transactionWithResult(Boolean::class) {
                push(Route.Route1)
            }.unwrapOrReturn { fail() }
            assertEquals(true, result)
        }
        delay(100)
        router.pop { withResult(true) }

        // Test wrong type
        launch {
            router.transactionWithResult(Boolean::class) {
                push(Route.Route1)
            }.unwrapOrReturn {
                assertTrue { it.error is RouteResultError.UnexpectedResultType }
                return@launch
            }
            fail()
        }
        delay(100)
        router.pop { withResult(Unit) }

        // Test pop without result
        launch {
            router.transactionWithResult(Boolean::class) {
                push(Route.Route1)
            }.unwrapOrReturn {
                assertTrue { it.error is RouteResultError.Cancelled }
                return@launch
            }
            fail()
        }
        delay(100)
        router.pop()
    }

    @Test
    fun testRouteResultPropagated() = runBlockingTest {
        val router = Router(Route.Home)
        var finalResult = 0

        coroutineScope {
            launch {
                finalResult = router
                    .transactionWithResult(Int::class) { push(Route.Route1) }
                    .unwrapOrReturn { fail() }
            }

            launch {
                delay(20)
                val result = router
                    .transactionWithResult(Int::class) { push(Route.Route1) }
                    .unwrapOrReturn { fail() }
                router.pop { withResult(result + 1) }
            }

            launch {
                delay(40)
                val result = router
                    .transactionWithResult(Int::class) { push(Route.Route1) }
                    .unwrapOrReturn { fail() }
                router.pop { withResult(result + 1) }
            }

            launch {
                delay(60)
                router.pop { withResult(1) }
            }
        }

        assertEquals(3, finalResult)
    }

    @Test
    fun testRouteResultDeep() = runBlockingTest {
        val router = Router(Route.Home)

        launch {
            delay(20)
            router.push(Route.Route2)
            router.push(Route.Route2)
            router.push(Route.Route2)
            router.push(Route.Route2)

            router.transaction {
                pop { toRoute(Route.Route1) }
                pop { withResult(true) }
            }
        }

        val result = router
            .transactionWithResult(Boolean::class) { push(Route.Route1) }
            .unwrapOrReturn { fail() }

        assertEquals(result, true)
    }
}

private class RouteLifecycleCounter(
    var created: Int = 0,
    var started: Int = 0,
    var stopped: Int = 0,
    var destroyed: Int = 0,
    var destroyedTransitionComplete: Int = 0,
)

private class CounterRouteLifecycleListener(
    val counter: RouteLifecycleCounter,
) : IRouteLifecycleListener {
    override fun onRouteCreated() {
        counter.created++
    }

    override fun onRouteStarted() {
        counter.started++
    }

    override fun onRouteStopped() {
        counter.stopped++
    }

    override fun onRouteDestroyed() {
        counter.destroyed++
    }

    override fun onRouteDestroyedTransitionComplete() {
        counter.destroyedTransitionComplete++
    }
}

private sealed class Route: IRoute {
    data object Home: Route()
    data object Route1: Route()
    data object Route2: Route()
    data class Test(val test: Int): Route()
}