package com.outsidesource.oskitkmp.bloc

import com.outsidesource.oskitkmp.concurrency.withDelay
import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class BlocTest {
    @Test
    fun getCurrentStateOnSubscribe() = runBlocking {
        val bloc = TestBloc()
        val result = bloc.stream().first()
        assertTrue(result.testInt == 0, "State not returned on subscription")
    }

    @Test
    fun stateGetter() = runBlocking {
        val bloc = TestBloc()
        bloc.increment()
        assertTrue(bloc.state.testInt == 1, "asserted state getter to return 1. Got ${bloc.state.testInt}")
    }

    @Test
    fun subscribe() = runBlocking {
        val bloc = TestBloc()
        var eventCount = 0

        val subscription = launch { bloc.stream().collect { eventCount++ } }
        delay(16)
        bloc.increment()
        delay(16)
        bloc.increment()
        delay(16)
        bloc.increment()
        delay(16)
        subscription.cancelAndJoin()
        assertTrue(eventCount == 4, "asserted 4 emits and got $eventCount")
    }

    @Test
    fun distinctUpdates() = runBlocking {
        val bloc = TestBloc()
        var eventCount = 0

        val subscription = launch { bloc.stream().collect { eventCount++ } }
        bloc.setString("Test")
        delay(16)
        bloc.setString("Test")
        delay(16)
        bloc.setString("Test 2")
        delay(16)
        bloc.setString("Test 2")
        delay(16)
        bloc.setString("")
        delay(16)
        bloc.setString("")
        delay(16)
        subscription.cancel()
        assertTrue(eventCount == 3, "asserted 3 emits and got $eventCount")
    }

    @Test
    fun onStart() = runBlocking {
        var onStartedCount = 0
        val bloc = TestBloc(onStartCallback = { onStartedCount++ })
        assertTrue(onStartedCount == 0, "On Started was called before subscription")
        bloc.stream().first()
        assertTrue(onStartedCount == 1, "On Started was not called")
        bloc.stream().first()
        assertTrue(onStartedCount == 2, "On Started was not called after dispose")
    }

    @Test
    fun onDispose() = runBlocking {
        var onDisposedCount = 0
        val bloc = TestBloc(onDisposeCallback = { onDisposedCount++ })
        assertTrue(onDisposedCount == 0, "onDispose was called before subscription")
        val subscription1 = launch { bloc.stream().collect { } }
        val subscription2 = launch { bloc.stream().collect { } }
        assertTrue(onDisposedCount == 0, "onDispose was called before subscription was cancelled")
        subscription1.cancelAndJoin()
        assertTrue(onDisposedCount == 0, "onDispose was called with an active subscription")
        subscription2.cancelAndJoin()
        assertTrue(onDisposedCount == 1, "onDispose was not called")
        bloc.stream().first()
        assertTrue(onDisposedCount == 2, "onDispose was not called after restart")
    }

    @Test
    fun persistStateOnDispose() = runBlocking {
        val bloc = TestBloc(retainStateOnDispose = false)
        bloc.increment()
        bloc.increment()
        assertTrue(bloc.state.testInt == 2, "State did not update")
        bloc.stream().first()
        assertTrue(bloc.state.testInt == 0, "State did not reset on dispose with persistStateOnDispose == false")

        val bloc2 = TestBloc(retainStateOnDispose = true)
        bloc2.increment()
        bloc2.increment()
        assertTrue(bloc2.state.testInt == 2, "State did not update")
        bloc2.stream().first()
        assertTrue(bloc2.state.testInt == 2, "State reset on dispose with persistStateOnDispose == true")
    }

    @Test
    fun blocScope() = runBlocking {
        val bloc = TestBloc()
        var jobCompleted = false

        val jobFuture = bloc.blocScope.launch {
            delay(1000)
            jobCompleted = true
        }

        bloc.stream().first()
        if (!jobFuture.isCancelled) fail("Bloc scope was not cancelled on dispose")
        if (jobCompleted) fail("Job completed")
    }

    @Test
    fun update() = runBlocking {
        val bloc = TestBloc()
        val originalState = bloc.state
        assertTrue(bloc.state.testInt == 0, "testInt was not 0")
        bloc.increment()
        bloc.increment()
        bloc.increment()
        bloc.increment()
        assertTrue(bloc.state.testInt == 4, "Update did not update state")
        assertTrue(originalState.testInt == 0, "Update is mutating in place instead of creating an immutable copy")
    }

    @Test
    fun effect() = runBlocking {
        val bloc = TestBloc()
        val start = Clock.System.now().toEpochMilliseconds()
        val result = bloc.testEffect()
        if (result !is Outcome.Ok || result.value.testInt != 1) fail("Effect did not return result")
        if (Clock.System.now().toEpochMilliseconds() - start < 500) fail("Effect did not wait 1000 milliseconds")
        if (bloc.state.testInt != 1) fail("Effect did not update state")
    }

    @Test
    fun effectCancel() = runBlocking {
        val bloc = TestBloc()

        // Cancellation on effect restart. Delays are important between calls to mimic human interaction and to
        // make sure race condition between starting and finishing an effect is checked
        launch { bloc.testEffect() }
        delay(100)
        launch { bloc.testEffect() }
        delay(100)
        launch { bloc.testEffect() }
        delay(100)
        launch { bloc.testEffect() }
        delay(100)
        launch { bloc.testEffect() }
        delay(100)
        launch { bloc.testEffect() }
        delay(100)
        launch { bloc.testEffect() }.join()

        if (bloc.state.testInt != 1)
            fail("Effects were not cancelled on effect launch. State was incremented to ${bloc.state.testInt}.")

        // Explicit cancellation
        launch {
            delay(16)
            bloc.cancelTestEffect()
        }
        val result = bloc.testEffect()
        if (result !is Outcome.Error || (result.error !is CancellationException)) fail("Effect did not cancel when explicitly cancelled")
    }

    @Test
    fun effectOnCancel() = runBlocking {
        val bloc = TestBloc()
        bloc.testEffect2()
        bloc.testEffect2()
        bloc.testEffect2()
        assertTrue(bloc.state.testInt == 3, "TestEffect2 not updating state properly. State: ${bloc.state.testInt}.")

        launch {
            delay(16)
            bloc.cancelTestEffect2()
        }

        bloc.testEffect2()
        delay(100)
        if (bloc.state.testInt != 0) fail("TestEffect2 onCancel did not work properly. State: ${bloc.state.testInt}.")
    }

    @Test
    fun effectOnDone() = runBlocking {
        val bloc = TestBloc()
        launch { bloc.testEffect3() }
        withDelay(16) { bloc.cancelTestEffect3() }.join()
        bloc.testEffect3()
        assertEquals(bloc.state.testInt, -3)
    }

    @Test
    fun effectCancelOnDispose() = runBlocking {
        // cancelOnDispose == true
        val bloc = TestBloc()
        val subscription = launch { bloc.stream().collect { } }
        val effectPromise = async { bloc.testEffect() }
        delay(16)
        subscription.cancelAndJoin()
        delay(200)
        val effectResult = effectPromise.await()
        assertTrue(bloc.state.testInt == 0,  "State should have been 0 due to dispose.")
        if (effectResult !is Outcome.Error || effectResult.error !is CancellationException)
            fail("Effect was not cancelled")

        // cancelOnDispose == false
        val subscription2 = launch { bloc.stream().collect {} }
        val effectPromise2 = async { bloc.noCancelOnDisposeEffect() }
        subscription2.cancelAndJoin()
        val effectResult2 = effectPromise2.await()
        assertTrue(bloc.state.testInt == 1,  "State should have been 1 due to cancelOnDispose == false")
        if (effectResult2 !is Outcome.Ok || effectResult2.value.testInt != 1) fail("Effect was cancelled")
    }

    @Test
    fun blocStatus() = runBlocking {
        val bloc = TestBloc()
        assertTrue(bloc.status == BlocStatus.Idle,  "Bloc Status was not Idle")
        val subscription = launch { bloc.stream().collect { } }
        delay(16)
        assertTrue(bloc.status == BlocStatus.Started,  "Bloc Status was not Started")
        subscription.cancelAndJoin()
        assertTrue(bloc.status == BlocStatus.Idle,  "Bloc Status was not Idle after dispose")
    }

    @Test
    fun computedValue() = runBlocking {
        val bloc = TestBloc()
        val subscription = launch { bloc.stream().collect { } }
        delay(100)
        assertTrue(bloc.state.testComputedInt == 2,  "Computed value not updated in constructor")
        bloc.increment()
        assertTrue(bloc.state.testComputedInt == 3,  "Computed value not updated in update")
        subscription.cancelAndJoin()
        assertTrue(bloc.state.testComputedInt == 2, "Computed value not updated after dispose")
    }

    @Test
    fun effectStatus() = runBlocking {
        val bloc = TestBloc()
        assertTrue(bloc.effectStatus(bloc::testEffect2) == EffectStatus.Idle, "Effect was not idle")
        val deferred = async { bloc.testEffect2() }
        delay(16)
        assertTrue(bloc.effectStatus(bloc::testEffect2) == EffectStatus.Running, "Effect was not running")
        deferred.await()
        assertTrue(bloc.effectStatus(bloc::testEffect2) == EffectStatus.Idle, "Effect was not idle after effect completion")
    }

    @Test
    fun testDependency() = runBlocking {
        var testBlocDisposed = false
        val testBloc = TestBloc(retainStateOnDispose = true, onDisposeCallback = { testBlocDisposed = true })
        testBloc.setString("dependency")
        testBloc.increment()
        val dependencyBloc = TestDependencyBloc(testBloc)

        // Test initial computed state without subscription
        assertTrue(dependencyBloc.state.dependentString == "dependency", "Computed initial dependent state was incorrect")

        // Test updated dependency state on first()
        testBloc.setString("dependency2")
        assertTrue(dependencyBloc.stream().first().dependentString == "dependency2", "Dependent state on first() was incorrect after dependent update")

        // Test computed dependency state with subscription
        val subDeferred = async { dependencyBloc.stream().drop(1).first() }
        delay(16)
        testBloc.increment()
        val subValue = subDeferred.await()
        assertTrue(subValue.dependentInt == 2, "Dependency update did not update parent value")

        // Test that dependencies are disposed
        delay(16)
        assertTrue(testBlocDisposed, "Dependency was not disposed")

        // Test Resubscribe
        val subDeferred2 = async { dependencyBloc.stream().drop(1).first() }
        delay(16)
        testBloc.increment()
        val subValue2 = subDeferred2.await()
        assertTrue(subValue2.dependentInt == 3, "Dependency update did not update parent value after resubscription")

        // Test distinct until changed
        var updateCount = 0
        val sub = launch { dependencyBloc.stream().drop(1).collect { updateCount++ } }
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
}

private data class TestState(val testString: String = "Test", val testInt: Int = 0, val testComputedInt: Int = 0)

private class TestBloc(
    retainStateOnDispose: Boolean = false,
    private val onStartCallback: (() -> Unit)? = null,
    private val onDisposeCallback: (() -> Unit)? = null,
) : Bloc<TestState>(TestState(), retainStateOnDispose = retainStateOnDispose) {

    override fun computed(state: TestState): TestState = state.copy(testComputedInt = state.testInt + 2)

    override fun onStart() = this.onStartCallback?.invoke() ?: Unit
    override fun onDispose() = this.onDisposeCallback?.invoke() ?: Unit

    fun increment() = update(state.copy(testInt = state.testInt + 1))
    private fun decrement() = update(state.copy(testInt = state.testInt - 1))

    fun setString(value: String) = update(state.copy(testString = value))

    suspend fun testEffect() = effect(
        id = "testEffect",
        block = {
            delay(500)
            Outcome.Ok(increment())
        }
    )

    suspend fun testEffect2(): Outcome<TestState> = effect(
        id = ::testEffect2,
        block = {
            delay(500)
            Outcome.Ok(increment())
        },
        onDone = fun(result) {
            if (!result.isCancelledJob()) return
            update(state.copy(testInt = 0))
        }
    )

    suspend fun testEffect3(): Outcome<TestState> = effect(
        id = ::testEffect3,
        block = {
            delay(500)
            Outcome.Ok(increment())
        },
        onDone = {
            decrement()
            decrement()
        }
    )

    suspend fun noCancelOnDisposeEffect() = effect(
        id = "noCancelOnDisposeEffect",
        cancelOnDispose = false,
        block = {
            delay(500)
            Outcome.Ok(this.increment())
        }
    )

    fun cancelTestEffect() = cancelEffect("testEffect")
    fun cancelTestEffect2() = cancelEffect(::testEffect2)
    fun cancelTestEffect3() = cancelEffect(::testEffect3)
}


private data class TestDependencyState(val count: Int = 0, val dependentString: String, val dependentInt: Int)

private class TestDependencyBloc(private val testBloc: TestBloc): Bloc<TestDependencyState>(
    initialState = TestDependencyState(dependentString = "", dependentInt = 0),
    retainStateOnDispose = true,
    dependencies = listOf(testBloc)
) {
    override fun computed(state: TestDependencyState): TestDependencyState = state.copy(
        dependentString = testBloc.state.testString,
        dependentInt = testBloc.state.testInt
    )

    fun set(value: Int) = update(state.copy(count = value))
}