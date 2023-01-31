package com.outsidesource.oskitkmp.interactor

import com.outsidesource.oskitkmp.devTool.OSDevTool
import com.outsidesource.oskitkmp.devTool.sendEvent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

interface IInteractorObservable<T : Any> {
    val state: T
    fun flow(lifetimeScope: CoroutineScope? = null): Flow<T>
}

/**
 * Interactor (Business Logic Component)
 * An isolated slice of safely mutable, observable state that encapsulates business logic pertaining to state
 * manipulation.
 *
 * Interactor Lifecycle
 * An Interactor's lifecycle is dependent on its observers. When the first observer subscribes to the Interactor [onStart] is called.
 * When the last observer unsubscribes [onDispose] is called. A Interactor may choose to reset its state when [onDispose]
 * is called by setting [retainStateOnDispose] to false. A Interactor will call [onStart] again if it gains a new
 * observer after it has been disposed. Likewise, a Interactor will call [onDispose] again if it loses those observers.
 *
 * Observing State
 * When an observer subscribes to state it will immediately receive the latest state as the first emit. Afterwards,
 * only changes to the state will be emitted to observers.
 *
 * Updating Interactor State
 * The only way to update a Interactor's state is by calling the [update] method. Calling [update] will synchronously update
 * the internal state with a new copy of state and notify all observers of the change as long as the new state is
 * different than the previous state.
 *
 * [initialState] The initial state of an Interactor.
 *
 * [retainStateOnDispose] If false, the internal state will be reset to [initialState] when the interactor is
 * disposed. If true, the Interactor's state will be retained until the Interactor is garbage collected.
 */
abstract class Interactor<T : Any>(
    private val initialState: T,
    private val retainStateOnDispose: Boolean = false,
    private val dependencies: List<Interactor<*>> = emptyList(),
) : IInteractorObservable<T> {
    private val subscriptionCount = atomic(0)
    private val dependencySubscriptionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state: MutableStateFlow<T> by lazy { MutableStateFlow(computed(initialState)) }

    /**
     * Provides a mechanism to allow launching [Job]s externally that follow the Interactor's lifecycle. All [Job]s launched
     * in [interactorScope] will be cancelled when the Interactor is disposed.
     */
    val interactorScope = CoroutineScope(
        defaultInteractorDispatcher + SupervisorJob() + CoroutineExceptionHandler { _, e -> e.printStackTrace() }
    )

    /**
     * Returns the current status of the Interactor
     */
    val status get() = if (subscriptionCount.value > 0) InteractorStatus.Started else InteractorStatus.Idle

    /**
     * Retrieves the current state of the Interactor.
     */
    override val state get() =
        if (dependencies.isNotEmpty() && subscriptionCount.value == 0) computed(_state.value) else _state.value

    init {
        OSDevTool.sendEvent(this::class.simpleName ?: "", "New Interactor", initialState)
    }

    /**
     * Returns the state as a stream/observable for observing updates. The latest state will be immediately emitted to
     * a new subscriber.
     * Collecting the flow adds a subscription dependency to the Interactor which is removed when the Flow collector is
     * cancelled unless a lifetimeScope is provided.
     *
     * [lifetimeScope] allows the interactor to only remove a subscription dependency when the scope has been cancelled.
     * This prevents premature Interactor disposal mainly during activity/fragment recreation due to configuration change.
     * Typically, viewModelScope is the most appropriate scope here.
     */
    override fun flow(lifetimeScope: CoroutineScope?): Flow<T> {
        if (dependencies.isNotEmpty()) _state.update { computed(it) }

        return _state
            .onStart { handleSubscribe(lifetimeScope) }
            .onCompletion { if (lifetimeScope == null) handleUnsubscribe() }
    }

    /**
     * Computes properties based on latest state for every update
     */
    protected open fun computed(state: T): T = state

    /**
     * Called when the interactor receives its first subscription. [onStart] will be called again if it gains an
     * observer after it has been disposed.
     *
     * This is a good time to new-up any services, subscribe to dependent interactors, or open any resource handlers.
     */
    protected open fun onStart() {}

    /**
     * Called when the last subscription is closed. [onDispose] will be called every time all observers have stopped
     * observing.
     *
     * This is a good place to close any resource handlers or services.
     */
    protected open fun onDispose() {}

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

    private fun handleSubscribe(lifetimeScope: CoroutineScope?) {
        checkShouldStart()
        lifetimeScope?.coroutineContext?.job?.invokeOnCompletion { handleUnsubscribe() }
        subscriptionCount.incrementAndGet()
    }

    private fun handleUnsubscribe() {
        subscriptionCount.decrementAndGet()
        checkShouldDispose()
    }

    private fun checkShouldStart() {
        if (subscriptionCount.value > 0) return

        dependencies.forEach { dependency ->
            dependencySubscriptionScope.launch {
                dependency.flow().drop(1).collect {
                    _state.update { computed(it) }
                }
            }
        }

        OSDevTool.sendEvent(this::class.simpleName ?: "", "Start", _state.value)
        onStart()
    }

    private fun checkShouldDispose() {
        if (subscriptionCount.value > 0) return

        interactorScope.coroutineContext.cancelChildren()
        dependencySubscriptionScope.coroutineContext.cancelChildren()
        if (!retainStateOnDispose) _state.update { computed(initialState) }

        OSDevTool.sendEvent(this::class.simpleName ?: "", "Dispose", _state.value)
        onDispose()
    }
}

/**
 * InteractorStatus
 * The current status of a Interactor. Idle represents a Interactor that is in a disposed/unused state (there are no
 * active subscriptions). Once there is an active subscription, the Interactor is in a Started state.
 */
enum class InteractorStatus {
    Started,
    Idle,
}

/**
 * Allows implementers to change the default thread effect management is run on.
 */
internal expect val defaultInteractorDispatcher: CoroutineDispatcher
