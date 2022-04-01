package com.outsidesource.oskitkmp.bloc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.outsidesource.oskitkmp.router.LocalRouteScope
import com.outsidesource.oskitkmp.router.RouteDestroyedEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map

internal actual val defaultBlocDispatcher: CoroutineDispatcher = Dispatchers.Default

@Composable
fun <B : Bloc<S>, S> rememberBloc(factory: () -> B): Pair<S, B> {
    val routeScope = LocalRouteScope.current
    RouteDestroyedEffect { routeScope.cancel() }

    val (bloc, stream) = remember {
        val bloc = factory()
        val stream = bloc.stream(routeScope)
        Pair(bloc, stream)
    }

    val state by stream.collectAsState(initial = bloc.state)
    return Pair(state, bloc)
}

@Composable
fun <B : Bloc<IS>, IS, OS> rememberBlocSelector(factory: () -> B, transform: (state: IS) -> OS): Pair<OS, B> {
    val routeScope = LocalRouteScope.current
    RouteDestroyedEffect { routeScope.cancel() }

    val (bloc, stream) = remember {
        val bloc = factory()
        val stream = bloc.stream(routeScope).map { transform(it) }
        Pair(bloc, stream)
    }

    val state by stream.collectAsState(initial = transform(bloc.state))
    return Pair(state, bloc)
}

@Composable
fun <BC : BlocCoordinator<S>, S> rememberBlocCoordinator(block: () -> BC): Pair<S, BC> {
    val routeScope = LocalRouteScope.current
    RouteDestroyedEffect { routeScope.cancel() }

    val (bc, stream) = remember {
        val bc = block()
        val stream = bc.stream(routeScope)
        Pair(bc, stream)
    }

    val state by stream.collectAsState(initial = bc.state)
    return Pair(state, bc)
}
