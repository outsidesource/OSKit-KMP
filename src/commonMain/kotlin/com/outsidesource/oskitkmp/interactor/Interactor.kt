package com.outsidesource.oskitkmp.interactor

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

interface IInteractor<T : Any> {
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
 * emit. Afterward, only changes to the state will be emitted to observers.
 *
 * Updating State
 * The only way to update an [Interactor]'s state is by calling the [update] method. Calling [update] will synchronously
 * update the internal state with a new copy of state and notify all observers of the change as long as the new state is
 * different from the previous state.
 *
 * [initialState] The initial state of an Interactor.
 *
 * [dependencies] A list of [Interactor]s this [Interactor] is dependent on. When any dependent [Interactor] is updated,
 * the [computed] function is called and the resulting state is emitted to all subscribers of this [Interactor].
 */
abstract class Interactor<T : Any>(
    private val initialState: T,
    private val dependencies: List<IInteractor<*>> = emptyList(),
) : IInteractor<T> {
    internal val subscriptionCount = atomic(0)
    internal val dependencySubscriptionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state: MutableStateFlow<T> by lazy { MutableStateFlow(computed(initialState)) }

    /**
     * Provides a standard coroutine scope for use in the Interactor.
     */
    protected val interactorScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, e -> e.printStackTrace() },
    )

    /**
     * Retrieves the current state of the Interactor.
     */
    override val state get() =
        if (dependencies.isNotEmpty() && subscriptionCount.value == 0) computed(_state.value) else _state.value

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
        return _state.updateAndGet { computed(function(it)) }
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
            // Wait for all dependencies to start collection
            allDependenciesSubscribedFlow.take(dependencies.size).collect()
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
 * Create a functional [Interactor] without having to extend the Interactor class. It is recommended to extend the
 * [Interactor] class directly, but sometimes that may not be possible. [createInteractor] provides an alternative
 * means of creating an [Interactor].
 *
 * Example:
 * ```
 * private interface ITestInteractor: IInteractor<TestState> {
 *     fun test()
 * }
 *
 * val interactor = createInteractor(
 *     initialState = TestState(),
 *     dependencies = emptyList(),
 *     computed = { state -> state.copy(testInt = state.testString.length) },
 *     hooks = { update, interactor ->
 *         object : ITestInteractor, IInteractor<TestState> by interactor {
 *             override fun test() {
 *                 update { state -> state.copy(testString = "Test Succeeded") }
 *             }
 *         }
 *     },
 * )
 * ```
 */
fun <S : Any, H : IInteractor<S>> createInteractor(
    initialState: S,
    dependencies: List<IInteractor<*>>,
    computed: (state: S) -> S,
    hooks: ((update: (state: S) -> S) -> S, interactor: IInteractor<S>) -> H,
): H {
    return object : Interactor<S>(
        initialState,
        dependencies,
    ) {
        val resolvedHooks = hooks(::update, this)
        override fun computed(state: S): S = computed(state)
    }.resolvedHooks
}