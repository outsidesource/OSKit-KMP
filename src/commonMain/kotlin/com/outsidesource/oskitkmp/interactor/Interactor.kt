package com.outsidesource.oskitkmp.interactor

import com.outsidesource.oskitkmp.devTool.OSDevTool
import com.outsidesource.oskitkmp.devTool.sendEvent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

interface IInteractorObservable<T : Any> {
    val state: T
    fun flow(): Flow<T>
}

/**
 * [Interactor]
 * An isolated slice of safely mutable, observable state that encapsulates business logic pertaining to state
 * manipulation.
 *
 * Observing State
 * When an observer subscribes to state via the [flow] method it will immediately receive the latest state as the first
 * emit. Afterwards, only changes to the state will be emitted to observers.
 *
 * Updating State
 * The only way to update an [Interactor]'s state is by calling the [update] method. Calling [update] will synchronously update
 * the internal state with a new copy of state and notify all observers of the change as long as the new state is
 * different from the previous state.
 *
 * [initialState] The initial state of an Interactor.
 *
 * [dependencies] A list of [Interactor]s this [Interactor] is dependent on. When any dependent [Interactor] is updated,
 * the [computed] function is called and the resulting state is emitted to all subscribers of this [Interactor].
 */
abstract class Interactor<T : Any>(
    private val initialState: T,
    private val dependencies: List<Interactor<*>> = emptyList(),
) : IInteractorObservable<T> {
    internal val subscriptionCount = atomic(0)
    internal val dependencySubscriptionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state: MutableStateFlow<T> by lazy { MutableStateFlow(computed(initialState)) }

    /**
     * Provides a standard coroutine scope for use in the Interactor.
     */
    protected val interactorScope = CoroutineScope(
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
     * Returns the state as a flow for observing updates. The latest state will be immediately emitted to a new
     * subscriber.
     */
    override fun flow(): Flow<T> = _state
        .onSubscription { handleSubscribe() }
        .onCompletion { handleUnsubscribe() }

    /**
     * Computes properties based on latest state for every update
     */
    protected open fun computed(state: T): T = state

    /**
     * Immutably update the state and notify all subscribers of the change.
     */
    protected fun update(function: (state: T) -> T): T {
        val updated = _state.updateAndGet { computed(function(it)) }
        OSDevTool.sendEvent(this::class.simpleName ?: "", "Updated", updated)
        return updated
    }

    private suspend fun handleSubscribe() {
        if (subscriptionCount.value > 0) {
            subscriptionCount.incrementAndGet()
            return
        }

        val allDependenciesSubscribedFlow = MutableSharedFlow<Unit>(dependencies.size)

        dependencies.forEach { dependency ->
            dependencySubscriptionScope.launch {
                dependency
                    .flow()
                    .drop(1)
                    .onStart { allDependenciesSubscribedFlow.emit(Unit) }
                    .collect { _state.update { computed(it) } }
            }
        }

        // This fixes a race condition of dependent state updating before the dependency
        // subscriptions have started collecting
        if (dependencies.isNotEmpty()) {
            allDependenciesSubscribedFlow.take(dependencies.size).collect() // Wait for all dependencies to start collection
            _state.update { computed(it) }
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
