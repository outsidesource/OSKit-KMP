package com.outsidesource.oskitkmp.interactor

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

internal actual val defaultInteractorDispatcher: CoroutineDispatcher = Dispatchers.Default

/**
 * rememberInteractor will remember a Interactor and subscribe to its state for a given lifetime.
 * The lifetime for the Interactor is the life of the route NOT the life of the composable.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(factory: () -> B): Pair<S, B> = rememberInteractor(
    rememberFactory = { remember { it() } },
    interactorFactory = factory,
)

/**
 * rememberInteractor will remember a Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(
    key1: Any? = Unit,
    factory: () -> B,
): Pair<S, B> {
    val scope = remember(key1) { CoroutineScope(Dispatchers.Default + Job()) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }

    return rememberInteractor(
        rememberFactory = { remember(key1) { it() } },
        interactorFactory = factory,
        lifetimeScope = scope,
    )
}

/**
 * rememberInteractor will remember a Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(
    key1: Any? = Unit,
    key2: Any? = Unit,
    factory: () -> B,
): Pair<S, B> {
    val scope = remember(key1, key2) { CoroutineScope(Dispatchers.Default + Job()) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }

    return rememberInteractor(
        rememberFactory = { remember(key1, key2) { it() } },
        interactorFactory = factory,
        lifetimeScope = scope,
    )
}

/**
 * rememberInteractor will remember an Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(
    key1: Any? = Unit,
    key2: Any? = Unit,
    key3: Any? = Unit,
    factory: () -> B,
): Pair<S, B> {
    val scope = remember(key1, key2) { CoroutineScope(Dispatchers.Default + Job()) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }

    return rememberInteractor(
        rememberFactory = { remember(key1, key2, key3) { it() } },
        interactorFactory = factory,
        lifetimeScope = scope
    )
}

/**
 * rememberInteractor will remember a Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
private fun <B : IInteractorObservable<S>, S> rememberInteractor(
    rememberFactory: @Composable (@DisallowComposableCalls () -> Pair<B, Flow<S>>) -> Pair<B, Flow<S>>,
    interactorFactory: () -> B,
    lifetimeScope: CoroutineScope? = null,
): Pair<S, B> {
    val streamScope = lifetimeScope ?: rememberCoroutineScope()

    val (interactor, stream) = rememberFactory {
        val interactor = interactorFactory()
        val stream = interactor.flow(streamScope)
        Pair(interactor, stream)
    }

    val state by stream.collectAsState(initial = interactor.state)
    return Pair(state, interactor)
}
