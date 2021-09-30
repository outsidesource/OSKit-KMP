package com.outsidesource.oskitkmp.bloc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val defaultBlocEffectDispatcher: CoroutineDispatcher = Dispatchers.Default

@Composable
fun <B : Bloc<S>, S> rememberBloc(factory: () -> B): Pair<S, B> {
    val (bloc, stream) = remember {
        val bloc = factory()
        val stream = bloc.stream()
        Pair(bloc, stream)
    }
    val state by stream.collectAsState(initial = bloc.state)
    return Pair(state, bloc)
}

@Composable
fun <BC : BlocCoordinator<S>, S> rememberBlocCoordinator(block: () -> BC): Pair<S, BC> {
    val (bc, stream) = remember {
        val bc = block()
        val stream = bc.stream
        Pair(bc, stream)
    }
    val state by stream.collectAsState(initial = bc.state)
    return Pair(state, bc)
}