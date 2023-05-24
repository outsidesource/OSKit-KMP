package com.outsidesource.oskitkmp.interactor

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class InteractorTest {
    @Test
    fun getCurrentStateOnSubscribe() = runBlocking {
        val interactor = TestInteractor()
        val result = interactor.flow().first()
        assertTrue(result.testInt == 0, "State not returned on subscription")
    }

    @Test
    fun stateGetter() = runBlocking {
        val interactor = TestInteractor()
        interactor.increment()
        assertTrue(interactor.state.testInt == 1, "asserted state getter to return 1. Got ${interactor.state.testInt}")
    }

    @Test
    fun subscribe() = runBlocking {
        val interactor = TestInteractor()
        var eventCount = 0

        val subscription = launch { interactor.flow().collect { eventCount++ } }
        delay(16)
        interactor.increment()
        delay(16)
        interactor.increment()
        delay(16)
        interactor.increment()
        delay(16)
        subscription.cancelAndJoin()
        assertTrue(eventCount == 4, "asserted 4 emits and got $eventCount")
    }

    @Test
    fun distinctUpdates() = runBlocking {
        val interactor = TestInteractor()
        var eventCount = 0

        val subscription = launch { interactor.flow().collect { eventCount++ } }
        interactor.setString("Test")
        delay(16)
        interactor.setString("Test")
        delay(16)
        interactor.setString("Test 2")
        delay(16)
        interactor.setString("Test 2")
        delay(16)
        interactor.setString("")
        delay(16)
        interactor.setString("")
        delay(16)
        subscription.cancel()
        assertTrue(eventCount == 3, "asserted 3 emits and got $eventCount")
    }

    @Test
    fun update() = runBlocking {
        val interactor = TestInteractor()
        val originalState = interactor.state
        assertTrue(interactor.state.testInt == 0, "testInt was not 0")
        interactor.increment()
        interactor.increment()
        interactor.increment()
        interactor.increment()
        assertTrue(interactor.state.testInt == 4, "Update did not update state")
        assertTrue(originalState.testInt == 0, "Update is mutating in place instead of creating an immutable copy")
    }

    @Test
    fun computedValue() = runBlocking {
        val interactor = TestInteractor()
        val subscription = launch { interactor.flow().collect { } }
        delay(100)
        assertTrue(interactor.state.testComputedInt == 2, "Computed value not updated in constructor")
        interactor.increment()
        assertTrue(interactor.state.testComputedInt == 3, "Computed value not updated in update")
        subscription.cancelAndJoin()
    }

    @Test
    fun testDependency() = runBlocking {
        val testBloc = TestInteractor()
        testBloc.setString("dependency")
        testBloc.increment()
        val dependencyBloc = TestDependencyInteractor(testBloc)

        // Test initial computed state without subscription
        assertTrue(dependencyBloc.state.dependentString == "dependency", "Computed initial dependent state was incorrect")

        // Test updated dependency state on first()
        testBloc.setString("dependency2")
        assertTrue(dependencyBloc.flow().first().dependentString == "dependency2", "Dependent state on first() was incorrect after dependent update")

        // Test updated dependency state with computed
        testBloc.setString("dependency3")
        assertTrue(dependencyBloc.state.dependentString == "dependency3", "Dependent state on state getter was incorrect after dependent update")

        // Test computed dependency state with subscription
        val subDeferred = async { dependencyBloc.flow().drop(1).first() }
        delay(16)
        testBloc.increment()
        val subValue = subDeferred.await()
        assertTrue(subValue.dependentInt == 2, "Dependency update did not update parent value")

        // Test Resubscribe
        val subDeferred2 = async { dependencyBloc.flow().drop(1).first() }
        delay(16)
        testBloc.increment()
        val subValue2 = subDeferred2.await()
        assertTrue(subValue2.dependentInt == 3, "Dependency update did not update parent value after resubscription")

        // Test distinct until changed
        var updateCount = 0
        val sub = launch { dependencyBloc.flow().drop(1).collect { updateCount++ } }
        delay(16)
        testBloc.setString("distinctTest1")
        delay(16)
        testBloc.setString("distinctTest2")
        delay(16)
        testBloc.setString("distinctTest2")
        delay(16)
        testBloc.setString("distinctTest2")
        sub.cancelAndJoin()
        assertTrue(updateCount == 2, "Dependency emitted update with equal value")
    }

    @Test
    fun testDependentInteractorDependencySubscriptionsCancelled() = runBlocking {
        val testBloc = TestInteractor()
        val dependencyBloc = TestDependencyInteractor(testBloc)
        assertTrue(dependencyBloc.subscriptionCount.value == 0, "Dependency subscription count should be 0")
        assertTrue(dependencyBloc.dependencySubscriptionScope.coroutineContext.job.children.count() == 0, "Dependency subscription count should be 0")
        val subscription = async { dependencyBloc.flow().collect {}}
        delay(16)
        assertTrue(dependencyBloc.subscriptionCount.value == 1, "Dependency subscription count should be 1")
        assertTrue(dependencyBloc.dependencySubscriptionScope.coroutineContext.job.children.count() == 1, "Dependency subscription count should be 1")
        val subscription2 = async { dependencyBloc.flow().collect {}}
        delay(16)
        assertTrue(dependencyBloc.subscriptionCount.value == 2, "Dependency subscription count should be 2")
        assertTrue(dependencyBloc.dependencySubscriptionScope.coroutineContext.job.children.count() == 1, "Dependency subscription count should be 1") // Only one because only one coroutine is launched for n subscriptions
        subscription.cancelAndJoin()
        subscription2.cancelAndJoin()
        delay(16)
        assertTrue(dependencyBloc.subscriptionCount.value == 0, "Dependency subscription count should be 0 after unsubscribe")
        assertTrue(dependencyBloc.dependencySubscriptionScope.coroutineContext.job.children.count() == 0, "Dependency subscription count should be 0")
    }

    @Test
    fun testDependencyComputedCalls() = runBlocking {
        var testBlocComputedCount = 0
        val testBloc = TestInteractor(onComputedCallback = { testBlocComputedCount++ })

        var dependencyBlocComputedCount = 0
        val dependencyBloc = TestDependencyInteractor(testBloc) { dependencyBlocComputedCount++ }

        testBloc.state
        testBloc.state
        assertTrue(testBlocComputedCount == 1, "Initial non dependent state computes more than once with no updates")

//        // This is a future optimization
//        dependencyBloc.state
//        dependencyBloc.state
//        assertTrue(dependencyBlocComputedCount == 1, "Dependent state computes more than once with no updates")
    }
}

private data class TestState(val testString: String = "Test", val testInt: Int = 0, val testComputedInt: Int = 0)

private class TestInteractor(
    private val onComputedCallback: (() -> Unit)? = null,
) : Interactor<TestState>(TestState()) {

    override fun computed(state: TestState): TestState {
        onComputedCallback?.invoke()
        return state.copy(testComputedInt = state.testInt + 2)
    }

    fun increment() = update { state -> state.copy(testInt = state.testInt + 1) }
    private fun decrement() = update { state -> state.copy(testInt = state.testInt - 1) }

    fun setString(value: String) = update { state -> state.copy(testString = value) }
}

private data class TestDependencyState(val count: Int = 0, val dependentString: String = "", val dependentInt: Int = 0)

private class TestDependencyInteractor(private val testBloc: TestInteractor, private val onComputedCallback: (() -> Unit)? = null) : Interactor<TestDependencyState>(
    initialState = TestDependencyState(dependentString = "", dependentInt = 0, count = 0),
    dependencies = listOf(testBloc)
) {
    override fun computed(state: TestDependencyState): TestDependencyState {
        onComputedCallback?.invoke()

        return state.copy(
            dependentString = testBloc.state.testString,
            dependentInt = testBloc.state.testInt,
        )
    }

    fun set(value: Int) = update { state -> state.copy(count = value) }
}
