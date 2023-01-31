package com.outsidesource.oskitkmp.interactor

import com.outsidesource.oskitkmp.devTool.OSDevTool
import com.outsidesource.oskitkmp.devTool.sendEvent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

interface IInteractorObservable<T : Any> {
    val state: T
    fun flow(): Flow<T>
}

/**
 * Interactor (Business Logic Component)
 * An isolated slice of safely mutable, observable state that encapsulates business logic pertaining to state
 * manipulation.
 *
 * Observing State
 * When an observer subscribes to state it will immediately receive the latest state as the first emit. Afterwards,
 * only changes to the state will be emitted to observers.
 *
 * Updating Interactor State
 * The only way to update an Interactor's state is by calling the [update] method. Calling [update] will synchronously update
 * the internal state with a new copy of state and notify all observers of the change as long as the new state is
 * different from the previous state.
 *
 * [initialState] The initial state of an Interactor.
 */
abstract class Interactor<T : Any>(
    private val initialState: T,
    private val dependencies: List<Interactor<*>> = emptyList(),
) : IInteractorObservable<T> {
    private val subscriptionCount = atomic(0)
    internal val dependencySubscriptionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state: MutableStateFlow<T> by lazy { MutableStateFlow(computed(initialState)) }

    /**
     * Provides a standard coroutine scope for use int the interactor.
     */
    val interactorScope = CoroutineScope(
        defaultInteractorDispatcher + SupervisorJob() + CoroutineExceptionHandler { _, e -> e.printStackTrace() }
    )

    /**
     * Retrieves the current state of the Interactor.
     */
    override val state get() =
        if (dependencies.isNotEmpty() && subscriptionCount.value == 0) computed(_state.value) else _state.value

    init {
        OSDevTool.sendEvent(this::class.simpleName ?: "", "New Interactor", initialState)
    }

    /**
     * Returns the state as a flow for observing updates. The latest state will be immediately emitted to
     * a new subscriber.
     * Collecting the flow adds a subscription dependency to the Interactor which is removed when the Flow collector is
     * cancelled
     */
    override fun flow(): Flow<T> {
        if (dependencies.isNotEmpty()) _state.update { computed(it) }

        return _state
            .onStart { handleSubscribe() }
            .onCompletion { handleUnsubscribe() }
    }

    /**
     * Computes properties based on latest state for every update
     */
    protected open fun computed(state: T): T = state

    /**
     * Immutably update the state and notify all subscribers of the change.
     * Note: This version of `update` is safe from race conditions when called concurrently from different threads
     * but may incur a performance penalty due to using `updateAndGet` under the hood which will recalculate if
     * the state has been changed by another thread. In certain circumstances it may be more performant to use a
     * Mutex or SynchronizedObject to concurrently update state.
     */
    protected fun update(function: (state: T) -> T): T {
        val updated = _state.updateAndGet { computed(function(it)) }
        OSDevTool.sendEvent(this::class.simpleName ?: "", "Updated", updated)
        return updated
    }

    private fun handleSubscribe() {
        if (subscriptionCount.value > 0) return

        dependencies.forEach { dependency ->
            dependencySubscriptionScope.launch {
                dependency.flow().drop(1).collect {
                    _state.update { computed(it) }
                }
            }
        }

        subscriptionCount.incrementAndGet()
    }

    private fun handleUnsubscribe() {
        subscriptionCount.decrementAndGet()
        if (subscriptionCount.value > 0) return

        dependencySubscriptionScope.coroutineContext.cancelChildren()
    }
}

/**
 * Allows implementers to change the default thread effect management is run on.
 */
internal expect val defaultInteractorDispatcher: CoroutineDispatcher
