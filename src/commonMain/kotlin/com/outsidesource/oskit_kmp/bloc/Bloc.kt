package com.outsidesource.oskit_kmp.bloc

import com.outsidesource.oskit_kmp.outcome.Outcome
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException


/**
 * Bloc (Business Logic Component)
 * An isolated slice of safely mutable, observable state that encapsulates business logic pertaining to state
 * manipulation.
 *
 * Bloc Lifecycle
 * A Bloc's lifecycle is dependent on its observers. When the first observer subscribes to the Bloc [onStart] is called.
 * When the last observer unsubscribes [onDispose] is called. A Bloc may choose to reset its state when [onDispose]
 * is called by setting [persistStateOnDispose] to false. A Bloc will call [onStart] again if it gains a new
 * observer after it has been disposed. Likewise, a Bloc will call [onDispose] again if it loses those observers.
 *
 * Observing State
 * When an observer subscribes to state it will immediately receive the latest state as the first emit. Afterwards,
 * only changes to the state will be emitted to observers.
 *
 * Updating Bloc State
 * The only way to update a Bloc's state is by calling the [update] method. Calling [update] will synchronously update
 * the internal state with a new copy of state and notify all observers of the change as long as the new state is
 * different than the previous state.
 *
 * Bloc Effects
 * Effects are asynchronous functions that update the state over time. An effect can be created with
 * an asynchronous function that calls [update] multiple times or by using the [effect] method. The [effect] method
 * provides a built-in cancellation mechanism. Calling an effect multiple times will cancel the previously started
 * effect and replace it with the new effect. The [effect] method also allows configuring whether or not the
 * effect should be cancelled when the Bloc is disposed or not.
 *
 * [initialState] The initial state of a Bloc.
 *
 * [persistStateOnDispose] If false, the internal state will be reset to [initialState] when the bloc is
 * disposed. If true, the Bloc's state will persist until the Bloc is garbage collected.
 */
abstract class Bloc<T : Any>(
    private val initialState: T,
    private val persistStateOnDispose: Boolean = false,
) {

    private val _effectsLock = SynchronizedObject()
    private val _dependentScopesLock = SynchronizedObject()

    private val _state: MutableStateFlow<T> by lazy { MutableStateFlow(computed(initialState)) }
    private val _effects: MutableMap<Any, CancellableEffect<*>> = mutableMapOf()
    private val _dependentScopes = mutableListOf<CoroutineScope>()
    private val _dependentCount = atomic(0)

    /**
     * Provides a mechanism to allow launching [Job]s externally that follow the Bloc's lifecycle. All [Job]s launched
     * in [blocScope] will be cancelled when the Bloc is disposed.
     */
    val blocScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Returns the current status of the Bloc
     */
    val status get() = if (_dependentCount.value > 0) BlocStatus.Started else BlocStatus.Idle

    /**
     * Retrieves the current state of the Bloc.
     */
    val state get() = _state.value

    /**
     * Returns the state as a stream/observable for observing updates. The latest state will be immediately emitted to
     * a new subscriber.
     * Collecting the flow adds a subscription dependency to the Bloc which is removed when the Flow collector is
     * cancelled unless a lifetimeScope is provided.
     *
     * [lifetimeScope] allows the bloc to only remove a subscription dependency when the scope has been cancelled.
     * This prevents premature Bloc disposal mainly during activity/fragment recreation due to configuration change.
     * Typically viewModelScope is the most appropriate scope here.
     */
    fun stream(lifetimeScope: CoroutineScope? = null): Flow<T> =
        _state.onStart { handleSubscribe(lifetimeScope) }.onCompletion {
            if (lifetimeScope == null) handleUnsubscribe()
        }

    /**
     * Computes properties based on latest state for every update
     */
    protected open fun computed(state: T): T = state

    /**
     * Called when the bloc receives its first subscription. [onStart] will be called again if it gains an
     * observer after it has been disposed.
     *
     * This is a good time to new-up any services, subscribe to dependent blocs, or open any resource handlers.
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
     * Runs a block of asynchronous code and provides a simple cancellation mechanism. If the effect is cancelled an
     * [Outcome.Error] will be returned with a [CancellationException] as its error value. When reusing effect IDs,
     * an ongoing effect will be cancelled and the passed block will run in its place. This can prevent the issue where
     * two of the same effect are called and the first call hangs for a few seconds while the second completes more
     * quickly.
     *
     * [id] The Identifier for the effect. This id is used for effect cancellation and querying effect status.
     * It is recommended to pass the calling effect function as the id.
     *
     * [context] The CoroutineContext to run the effect on. If no CoroutineContext is passed, the caller's
     * coroutineContext is inherited.
     *
     * [cancelOnDispose] if true, the effect will be cancelled when the Bloc is disposed if the effect is still running.
     *
     * [onDone] a block of synchronous code to be run when the effect finishes regardless of success or failure. This
     * can be used to update state regardless of if an effect is cancelled or not. NOTE: It is not guaranteed that
     * [onDone] will run before disposal and resetting of state if [persistStateOnDispose === false] so be careful
     * when updating state.
     */
    protected suspend fun <T> effect(
        id: Any,
        cancelOnDispose: Boolean = true,
        context: CoroutineContext = Dispatchers.Default,
        onDone: ((result: Outcome<T>) -> Unit)? = null,
        block: suspend () -> Outcome<T>,
    ): Outcome<T> = withContext(defaultBlocEffectDispatcher) {
        cancelEffect(id)

        try {
            val effect = CancellableEffect(cancelOnDispose = cancelOnDispose, job = async { block() }, onDone = onDone)
            synchronized(_effectsLock) { _effects[id] = effect }
            val result = withContext(context) { effect.run() }
            synchronized(_effectsLock) { if (_effects[id] == effect) _effects.remove(id) }
            result
        } catch (e: CancellationException) {
            Outcome.Error(e)
        }
    }

    /**
     * Returns the current status of an effect
     */
    fun effectStatus(id: Any): EffectStatus {
        return if (this._effects.containsKey(id)) EffectStatus.Running else EffectStatus.Idle
    }

    /**
     * Cancels an effect with the given [id].
     */
    protected fun cancelEffect(id: Any) {
        _effects[id]?.cancel()
        synchronized(_effectsLock) { _effects.remove(id) }
    }

    /**
     * Immutably update the state and notify all subscribers of the change.
     */
    protected fun update(state: T): T {
        _state.value = computed(state)
        return _state.value
    }

    private fun handleSubscribe(lifetimeScope: CoroutineScope?) {
        checkShouldStart()

        if (lifetimeScope != null) {
            synchronized(_dependentScopesLock) {
                if (_dependentScopes.contains(lifetimeScope)) return
                _dependentScopes.add(lifetimeScope)
            }

            lifetimeScope.coroutineContext.job.invokeOnCompletion {
                synchronized(_dependentScopesLock) { _dependentScopes.remove(lifetimeScope) }
                CoroutineScope(Dispatchers.Default).launch { handleUnsubscribe() }
            }
        }
        _dependentCount.incrementAndGet()
    }

    private fun handleUnsubscribe() {
        _dependentCount.decrementAndGet()
        checkShouldDispose()
    }

    private fun checkShouldStart() {
        if (_dependentCount.value > 0) return
        onStart()
    }

    private fun checkShouldDispose() {
        if (_dependentCount.value > 0) return

        _effects.values.forEach { if (it.cancelOnDispose) it.cancel() }
        synchronized(_effectsLock) { _effects.clear() }
        blocScope.coroutineContext.cancelChildren()
        synchronized(_dependentScopesLock) { _dependentScopes.clear() }
        if (!persistStateOnDispose) _state.value = computed(initialState)
        onDispose()
    }
}

/**
 * BlocStatus
 * The current status of a Bloc. Idle represents a Bloc that is in a disposed/unused state (there are no
 * active subscriptions). Once there is an active subscription, the Bloc is in a Started state.
 */
enum class BlocStatus {
    Started,
    Idle,
}

/**
 * EffectStatus
 * The current status of an effect. Idle represents an effect that is not running.
 */
enum class EffectStatus {
    Idle,
    Running,
}

internal data class CancellableEffect<T>(
    val cancelOnDispose: Boolean,
    private val job: Deferred<Outcome<T>>,
    private val onDone: ((result: Outcome<T>) -> Unit)?,
) {
    fun cancel() = job.cancel()

    suspend fun run(): Outcome<T> {
        return try {
            val result = job.await()
            onDone?.invoke(result)
            result
        } catch (e: CancellationException) {
            val result = Outcome.Error(e)
            onDone?.invoke(result)
            result
        }
    }
}

/**
 * Checks if an effect result is a cancelled effect result
 */
fun <T> Outcome<T>.isCancelledJob(): Boolean = this is Outcome.Error && this.error is CancellationException

/**
 * Allows implementers to change the default thread effect management is run on.
 */
internal expect val defaultBlocEffectDispatcher: CoroutineDispatcher