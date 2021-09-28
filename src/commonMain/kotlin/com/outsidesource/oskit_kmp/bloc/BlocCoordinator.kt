package com.outsidesource.oskit_kmp.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

abstract class BlocCoordinator<S>(private val scope: CoroutineScope) {
    internal abstract val proxy: StateFlow<S>
    abstract val state: S

    val stream: StateFlow<S> by lazy { proxy }
    open fun react(state: S) {}
}

abstract class BlocCoordinator2<D1 : Any, D2 : Any, S : Any>(
    scope: CoroutineScope,
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
) : BlocCoordinator<S>(scope) {
    protected abstract fun transform(s1: D1, s2: D2): S
    final override val state: S get() = transform(d1.state, d2.state)
    override val proxy =
        combine(d1.stream(scope), d2.stream(scope), ::transform).stateIn(scope, SharingStarted.Eagerly, state)
            .also { react(it.value) }
}

abstract class BlocCoordinator3<D1 : Any, D2 : Any, D3 : Any, S : Any>(
    scope: CoroutineScope,
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
    private val d3: Bloc<D3>,
) : BlocCoordinator<S>(scope) {
    protected abstract fun transform(s1: D1, s2: D2, s3: D3): S
    final override val state: S get() = transform(d1.state, d2.state, d3.state)
    override val proxy = combine(d1.stream(scope), d2.stream(scope), d3.stream(scope), ::transform)
        .stateIn(scope, SharingStarted.Eagerly, state)
        .also { react(it.value) }
}

abstract class BlocCoordinator4<D1 : Any, D2 : Any, D3 : Any, D4 : Any, S : Any>(
    scope: CoroutineScope,
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
    private val d3: Bloc<D3>,
    private val d4: Bloc<D4>,
) : BlocCoordinator<S>(scope) {
    protected abstract fun transform(s1: D1, s2: D2, s3: D3, s4: D4): S
    final override val state: S get() = transform(d1.state, d2.state, d3.state, d4.state)
    override val proxy =
        combine(d1.stream(scope), d2.stream(scope), d3.stream(scope), d4.stream(scope), ::transform)
            .stateIn(scope, SharingStarted.Eagerly, state)
            .also { react(it.value) }
}

abstract class BlocCoordinator5<D1 : Any, D2 : Any, D3 : Any, D4 : Any, D5 : Any, S>(
    scope: CoroutineScope,
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
    private val d3: Bloc<D3>,
    private val d4: Bloc<D4>,
    private val d5: Bloc<D5>,
) : BlocCoordinator<S>(scope) {
    protected abstract fun transform(s1: D1, s2: D2, s3: D3, s4: D4, s5: D5): S
    final override val state: S get() = transform(d1.state, d2.state, d3.state, d4.state, d5.state)
    override val proxy = combine(
        d1.stream(scope),
        d2.stream(scope),
        d3.stream(scope),
        d4.stream(scope),
        d5.stream(scope),
        ::transform
    )
        .stateIn(scope, SharingStarted.Eagerly, state)
        .also { react(it.value) }
}
