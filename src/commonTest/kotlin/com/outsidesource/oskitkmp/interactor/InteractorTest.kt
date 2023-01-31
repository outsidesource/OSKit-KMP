package com.outsidesource.oskitkmp.interactor

import kotlinx.coroutines.*
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
    fun onStart() = runBlocking {
        var onStartedCount = 0
        val interactor = TestInteractor(onStartCallback = { onStartedCount++ })
        assertTrue(onStartedCount == 0, "On Started was called before subscription")
        interactor.flow().first()
        assertTrue(onStartedCount == 1, "On Started was not called")
        interactor.flow().first()
        assertTrue(onStartedCount == 2, "On Started was not called after dispose")
    }

    @Test
    fun onDispose() = runBlocking {
        var onDisposedCount = 0
        val interactor = TestInteractor(onDisposeCallback = { onDisposedCount++ })
        assertTrue(onDisposedCount == 0, "onDispose was called before subscription")
        val subscription1 = launch { interactor.flow().collect { } }
        val subscription2 = launch { interactor.flow().collect { } }
        assertTrue(onDisposedCount == 0, "onDispose was called before subscription was cancelled")
        subscription1.cancelAndJoin()
        assertTrue(onDisposedCount == 0, "onDispose was called with an active subscription")
        subscription2.cancelAndJoin()
        assertTrue(onDisposedCount == 1, "onDispose was not called")
        interactor.flow().first()
        assertTrue(onDisposedCount == 2, "onDispose was not called after restart")
    }

    @Test
    fun testLifetimeScope() = runBlocking {
        var onDisposedCount = 0
        val interactor = TestInteractor(onDisposeCallback = { onDisposedCount++ })
        val testScope = CoroutineScope(Dispatchers.Default + Job())
        val subscription1 = launch { interactor.flow(testScope).collect {} }
        val subscription2 = launch { interactor.flow(testScope).collect {} }
        delay(16)
        subscription1.cancel()
        subscription2.cancel()
        delay(16)
        assertTrue(onDisposedCount == 0, "Interactor disposed even though lifetime scope was not cancelled")
        testScope.launch { interactor.flow(testScope).collect {} }
        testScope.launch { interactor.flow(testScope).collect {} }
        delay(16)
        testScope.cancel()
        delay(16)
        assertTrue(onDisposedCount == 1, "Interactor was not disposed when lifetime scope was cancelled")
    }

    @Test
    fun interactorScope() = runBlocking {
        val interactor = TestInteractor()
        var jobCompleted = false

        val jobFuture = interactor.lifecycleScope.launch {
            delay(1000)
            jobCompleted = true
        }

        interactor.flow().first()
        if (!jobFuture.isCancelled) fail("Interactor scope was not cancelled on dispose")
        if (jobCompleted) fail("Job completed")
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
    fun interactorStatus() = runBlocking {
        val interactor = TestInteractor()
        assertTrue(interactor.status == InteractorStatus.Idle, "Interactor Status was not Idle")
        val subscription = launch { interactor.flow().collect { } }
        delay(16)
        assertTrue(interactor.status == InteractorStatus.Started, "Interactor Status was not Started")
        subscription.cancelAndJoin()
        assertTrue(interactor.status == InteractorStatus.Idle, "Interactor Status was not Idle after dispose")
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
        var testBlocDisposed = false
        val testBloc = TestInteractor(onDisposeCallback = { testBlocDisposed = true })
        testBloc.setString("dependency")
        testBloc.increment()
        val dependencyBloc = TestDependencyInteractor(testBloc)

        // Test initial computed state without subscription
        assertTrue(dependencyBloc.state.dependentString == "dependency", "Computed initial dependent state was incorrect")

        // Test updated dependency state on first()
        testBloc.setString("dependency2")
        assertTrue(dependencyBloc.flow().first().dependentString == "dependency2", "Dependent state on first() was incorrect after dependent update")

        // Test computed dependency state with subscription
        val subDeferred = async { dependencyBloc.flow().drop(1).first() }
        delay(16)
        testBloc.increment()
        val subValue = subDeferred.await()
        assertTrue(subValue.dependentInt == 2, "Dependency update did not update parent value")

        // Test that dependencies are disposed
        delay(16)
        assertTrue(testBlocDisposed, "Dependency was not disposed")

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
    private val onStartCallback: (() -> Unit)? = null,
    private val onDisposeCallback: (() -> Unit)? = null,
    private val onComputedCallback: (() -> Unit)? = null,
) : Interactor<TestState>(TestState()) {

    override fun computed(state: TestState): TestState {
        onComputedCallback?.invoke()
        return state.copy(testComputedInt = state.testInt + 2)
    }

    override fun onStart() = this.onStartCallback?.invoke() ?: Unit
    override fun onDispose() = this.onDisposeCallback?.invoke() ?: Unit

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
