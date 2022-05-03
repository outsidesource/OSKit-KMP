package com.outsidesource.oskitkmp.bloc

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal actual val defaultBlocDispatcher: CoroutineDispatcher = Dispatchers.Default

internal class BlocViewModel : ViewModel()

/**
 * rememberBloc will remember a Bloc and subscribe to its state for a given lifetime.
 * The lifetime for the Bloc is the life of the route NOT the life of the composable.
 */
@Composable
fun <B : Bloc<S>, S> rememberBloc(factory: () -> B): Pair<S, B> = rememberBloc(
    rememberFactory = { remember { it() } },
    blocFactory = factory,
)

/**
 * rememberBloc will remember a Bloc and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : Bloc<S>, S> rememberBloc(
    key1: Any? = Unit,
    factory: () -> B,
): Pair<S, B> {
    val scope = remember(key1) { CoroutineScope(Dispatchers.Default + Job()) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }

    return rememberBloc(
        rememberFactory = { remember(key1) { it() } },
        blocFactory = factory,
        scope = scope,
    )
}

/**
 * rememberBloc will remember a Bloc and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : Bloc<S>, S> rememberBloc(
    key1: Any? = Unit,
    key2: Any? = Unit,
    factory: () -> B,
): Pair<S, B> {
    val scope = remember(key1, key2) { CoroutineScope(Dispatchers.Default + Job()) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }

    return rememberBloc(
        rememberFactory = { remember(key1, key2) { it() } },
        blocFactory = factory,
        scope = scope,
    )
}

/**
 * rememberBloc will remember a Bloc and subscribe to its state for the lifetime of the keys provided.
 */
@Composable
fun <B : Bloc<S>, S> rememberBloc(
    key1: Any? = Unit,
    key2: Any? = Unit,
    key3: Any? = Unit,
    factory: () -> B,
): Pair<S, B> {
    val scope = remember(key1, key2) { CoroutineScope(Dispatchers.Default + Job()) }
    DisposableEffect(scope) { onDispose { scope.cancel() } }

    return rememberBloc(
        rememberFactory = { remember(key1, key2, key3) { it() } },
        blocFactory = factory,
        scope = scope
    )
}

@Composable
private fun <B : Bloc<S>, S> rememberBloc(
    rememberFactory: @Composable (@DisallowComposableCalls () -> Pair<B, Flow<S>>) -> Pair<B, Flow<S>>,
    blocFactory: () -> B,
    scope: CoroutineScope? = null,
): Pair<S, B> {
    val streamScope = scope ?: viewModel<BlocViewModel>().viewModelScope

    val (bloc, stream) = rememberFactory {
        val bloc = blocFactory()
        val stream = bloc.stream(streamScope)
        Pair(bloc, stream)
    }

    val state by stream.collectAsState(initial = bloc.state)
    return Pair(state, bloc)
}

@Composable
fun <B : Bloc<IS>, IS, OS> rememberBlocSelector(factory: () -> B, transform: (state: IS) -> OS): Pair<OS, B> {
    val viewModel = viewModel<BlocViewModel>()
    val (bloc, stream) = remember {
        val bloc = factory()
        val stream = bloc.stream(viewModel.viewModelScope).map { transform(it) }
        Pair(bloc, stream)
    }
    val state by stream.collectAsState(initial = transform(bloc.state))
    return Pair(state, bloc)
}

@Composable
fun <BC : BlocCoordinator<S>, S> rememberBlocCoordinator(block: () -> BC): Pair<S, BC> {
    val viewModel = viewModel<BlocViewModel>()
    val (bc, stream) = remember {
        val bc = block()
        val stream = bc.stream(viewModel.viewModelScope)
        Pair(bc, stream)
    }
    val state by stream.collectAsState(initial = bc.state)
    return Pair(state, bc)
}
