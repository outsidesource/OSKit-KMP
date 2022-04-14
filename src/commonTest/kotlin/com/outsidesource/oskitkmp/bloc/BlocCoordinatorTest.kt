package com.outsidesource.oskitkmp.bloc

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.test.Test
import kotlin.test.assertTrue

class BlocCoordinatorTest {
    @Test
    fun coordinatorInitialValue() = runBlocking {
        val c = TestCoordinator(TestBloc1(), TestBloc2())
        assertTrue(c.state.one == 0 && c.state.two == "", "Initial State was wrong")
    }

    @Test
    fun coordinatorUpdatedValue() = runBlocking {
        val c = TestCoordinator(TestBloc1(), TestBloc2())

        c.increment()
        c.increment()
        c.increment()
        c.increment()

        assertTrue(c.state.one == 4, "Updated state was wrong")
    }

    @Test
    fun coordinatorSubscription() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(TestBloc1(), TestBloc2())
        var events = 0
        val sub = launch { c.stream(scope).collect { events++ } }

        delay(16)
        c.increment()
        delay(16)
        c.increment()
        delay(16)
        c.increment()
        delay(16)
        c.increment()
        delay(16)

        assertTrue(events == 5, "Subscription didn't receive 5 events")
        sub.cancelAndJoin()
    }

    @Test
    fun coordinatorSubscriptionDistinct() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(TestBloc1(), TestBloc2())
        var events = 0
        val sub = launch { c.stream(scope).collect { events++ } }

        delay(100)
        c.setValue("one")
        delay(100)
        c.setValue("two")
        delay(100)
        c.setValue("two")
        delay(100)
        c.setValue("two")
        delay(100)
        c.setValue("three")
        delay(100)

        assertTrue(events == 4, "Distinct did not work. Received $events events instead of 4")
        assertTrue(c.state.two == "three", "End state was not correct")
        sub.cancelAndJoin()
    }

    @Test
    fun unsubscriptionDispose() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(TestBloc1(), TestBloc2())
        val sub = scope.launch { c.stream(scope).collect { } }
        val sub2 = scope.launch { c.stream(scope).collect { } }

        c.increment()
        c.increment()
        c.increment()
        c.increment()

        sub.cancelAndJoin()
        assertTrue(c.state.one == 4, "Bloc disposed before all subscriptions were done")
        sub2.cancelAndJoin()
        scope.cancel()
        delay(16)
        assertTrue(c.state.one == 0, "Bloc did not reset on dispose from coordinator")
    }
}

private data class TestBCState(
    val one: Int,
)

private data class TestBCState2(
    val two: String,
)

private data class TestBCCombinedState(
    val one: Int,
    val two: String,
)

private class TestBloc1 : Bloc<TestBCState>(TestBCState(0), retainStateOnDispose = false) {
    fun increment() = update(state.copy(one = state.one + 1))
}

private class TestBloc2 : Bloc<TestBCState2>(TestBCState2(two = ""), retainStateOnDispose = false) {
    fun setValue(value: String) = update(state.copy(two = value))
}

private class TestCoordinator(private val bloc1: TestBloc1, private val bloc2: TestBloc2) :
    BlocCoordinator2<TestBCState, TestBCState2, TestBCCombinedState>(bloc1, bloc2) {

    override fun transform(s1: TestBCState, s2: TestBCState2) = TestBCCombinedState(one = s1.one, two = s2.two)

    fun increment() = this.bloc1.increment()
    fun setValue(value: String) = this.bloc2.setValue(value)
}
