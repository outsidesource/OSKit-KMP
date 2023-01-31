package com.outsidesource.oskitkmp.interactor

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

internal actual val defaultInteractorDispatcher: CoroutineDispatcher = Dispatchers.Default

/**
 * rememberInteractor will remember an Interactor and subscribe to its state.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(factory: () -> B): Pair<S, B> = rememberInteractor(
    rememberFactory = { remember { it() } },
    interactorFactory = factory,
)

/**
 * rememberInteractor will remember an Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(
    key1: Any? = Unit,
    factory: () -> B,
): Pair<S, B> = rememberInteractor(
    rememberFactory = { remember(key1) { it() } },
    interactorFactory = factory,
)

/**
 * rememberInteractor will remember an Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(
    key1: Any? = Unit,
    key2: Any? = Unit,
    factory: () -> B,
): Pair<S, B> = rememberInteractor(
    rememberFactory = { remember(key1, key2) { it() } },
    interactorFactory = factory,
)

/**
 * rememberInteractor will remember an Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : IInteractorObservable<S>, S> rememberInteractor(
    key1: Any? = Unit,
    key2: Any? = Unit,
    key3: Any? = Unit,
    factory: () -> B,
): Pair<S, B> = rememberInteractor(
    rememberFactory = { remember(key1, key2, key3) { it() } },
    interactorFactory = factory,
)

/**
 * rememberInteractor will remember an Interactor and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
private fun <B : IInteractorObservable<S>, S> rememberInteractor(
    rememberFactory: @Composable (@DisallowComposableCalls () -> Pair<B, Flow<S>>) -> Pair<B, Flow<S>>,
    interactorFactory: () -> B,
): Pair<S, B> {
    val (interactor, flow) = rememberFactory {
        val interactor = interactorFactory()
        Pair(interactor, interactor.flow())
    }

    val state by flow.collectAsState(initial = interactor.state)
    return Pair(state, interactor)
}
