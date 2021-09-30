package com.outsidesource.oskitkmp.bloc

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.test.Test
import kotlin.test.assertTrue

class BlocCoordinatorTest {
    @Test
    fun coordinatorInitialValue() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(scope, TestBloc1(), TestBloc2())
        assertTrue(c.state.one == 0 && c.state.two == "", "Initial State was wrong")
    }

    @Test
    fun coordinatorUpdatedValue() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(scope, TestBloc1(), TestBloc2())

        c.increment()
        c.increment()
        c.increment()
        c.increment()

        assertTrue(c.state.one == 4, "Updated state was wrong")
    }

    @Test
    fun coordinatorSubscription() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(scope, TestBloc1(), TestBloc2())
        var events = 0
        val sub = launch { c.stream.collect { events++ } }

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
        val c = TestCoordinator(scope, TestBloc1(), TestBloc2())
        var events = 0
        val sub = launch { c.stream.collect { events++ } }

        delay(16)
        c.setValue("one")
        delay(16)
        c.setValue("two")
        delay(16)
        c.setValue("two")
        delay(16)
        c.setValue("two")
        delay(16)
        c.setValue("three")
        delay(16)

        assertTrue(events == 4 && c.state.two == "three", "Distinct did not work")
        sub.cancelAndJoin()
    }

    @Test
    fun unsubscriptionDispose() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val c = TestCoordinator(scope, TestBloc1(), TestBloc2())
        val sub = scope.launch { c.stream.collect { } }
        val sub2 = scope.launch { c.stream.collect { } }

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

private class TestBloc1 : Bloc<TestBCState>(TestBCState(0), persistStateOnDispose = false) {
    fun increment() = update(state.copy(one = state.one + 1))
}

private class TestBloc2 : Bloc<TestBCState2>(TestBCState2(two = ""), persistStateOnDispose = false) {
    fun setValue(value: String) = update(state.copy(two = value))
}

private class TestCoordinator(private val scope: CoroutineScope, private val bloc1: TestBloc1, private val bloc2: TestBloc2) :
    BlocCoordinator2<TestBCState, TestBCState2, TestBCCombinedState>(scope, bloc1, bloc2) {

    override fun transform(s1: TestBCState, s2: TestBCState2) = TestBCCombinedState(one = s1.one, two = s2.two)

    fun increment() = this.bloc1.increment()
    fun setValue(value: String) = this.bloc2.setValue(value)
}
