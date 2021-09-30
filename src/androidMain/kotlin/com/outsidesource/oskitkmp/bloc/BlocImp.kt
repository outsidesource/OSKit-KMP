package com.outsidesource.oskitkmp.bloc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal actual val defaultBlocEffectDispatcher: CoroutineDispatcher = Dispatchers.Default

internal class BlocViewModel : ViewModel()

@Composable
fun <B : Bloc<S>, S> rememberBloc(factory: () -> B): Pair<S, B> {
    val viewModel = viewModel<BlocViewModel>()
    val (bloc, stream) = remember {
        val bloc = factory()
        val stream = bloc.stream(viewModel.viewModelScope)
        Pair(bloc, stream)
    }
    val state by stream.collectAsState(initial = bloc.state)
    return Pair(state, bloc)
}

@Composable
fun <BC : BlocCoordinator<S>, S> rememberBlocCoordinator(block: (scope: CoroutineScope) -> BC): Pair<S, BC> {
    val viewModel = viewModel<BlocViewModel>()
    val (bc, stream) = remember {
        val bc = block(viewModel.viewModelScope)
        val stream = bc.stream
        Pair(bc, stream)
    }
    val state by stream.collectAsState(initial = bc.state)
    return Pair(state, bc)
}