package com.outsidesource.oskitkmp.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

abstract class BlocCoordinator<S : Any> {
    internal abstract fun proxy(scope: CoroutineScope): StateFlow<S>
    abstract val state: S

    fun stream(lifetimeScope: CoroutineScope): StateFlow<S> = proxy(lifetimeScope)
    open fun react(state: S) {}
}

abstract class BlocCoordinator2<D1 : Any, D2 : Any, S : Any>(
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
) : BlocCoordinator<S>() {
    protected abstract fun transform(s1: D1, s2: D2): S
    final override val state: S get() = transform(d1.state, d2.state)
    override fun proxy(scope: CoroutineScope) =
        combine(d1.stream(scope), d2.stream(scope), ::transform).stateIn(scope, SharingStarted.Eagerly, state)
            .also { react(it.value) }
}

abstract class BlocCoordinator3<D1 : Any, D2 : Any, D3 : Any, S : Any>(
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
    private val d3: Bloc<D3>,
) : BlocCoordinator<S>() {
    protected abstract fun transform(s1: D1, s2: D2, s3: D3): S
    final override val state: S get() = transform(d1.state, d2.state, d3.state)
    override fun proxy(scope: CoroutineScope) =
        combine(d1.stream(scope), d2.stream(scope), d3.stream(scope), ::transform)
            .stateIn(scope, SharingStarted.Eagerly, state)
            .also { react(it.value) }
}

abstract class BlocCoordinator4<D1 : Any, D2 : Any, D3 : Any, D4 : Any, S : Any>(
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
    private val d3: Bloc<D3>,
    private val d4: Bloc<D4>,
) : BlocCoordinator<S>() {
    protected abstract fun transform(s1: D1, s2: D2, s3: D3, s4: D4): S
    final override val state: S get() = transform(d1.state, d2.state, d3.state, d4.state)
    override fun proxy(scope: CoroutineScope) =
        combine(d1.stream(scope), d2.stream(scope), d3.stream(scope), d4.stream(scope), ::transform)
            .stateIn(scope, SharingStarted.Eagerly, state)
            .also { react(it.value) }
}

abstract class BlocCoordinator5<D1 : Any, D2 : Any, D3 : Any, D4 : Any, D5 : Any, S : Any>(
    private val d1: Bloc<D1>,
    private val d2: Bloc<D2>,
    private val d3: Bloc<D3>,
    private val d4: Bloc<D4>,
    private val d5: Bloc<D5>,
) : BlocCoordinator<S>() {
    protected abstract fun transform(s1: D1, s2: D2, s3: D3, s4: D4, s5: D5): S
    final override val state: S get() = transform(d1.state, d2.state, d3.state, d4.state, d5.state)
    override fun proxy(scope: CoroutineScope) = combine(
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

abstract class BlocCoordinatorN<S : Any>(private vararg val d: Bloc<Any>) : BlocCoordinator<S>() {
    protected abstract fun transform(s: Array<Any>): S
    final override val state: S get() = transform(d.map { it.state }.toTypedArray())
    override fun proxy(scope: CoroutineScope) =
        combine(d.map { it.stream(scope) }, ::transform).stateIn(scope, SharingStarted.Eagerly, state)
                .also { react(it.value) }
    }
    