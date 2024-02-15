package com.outsidesource.oskitkmp.lib

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * A Lazy value initialized with a parameter
 */
@Suppress("UNCHECKED_CAST")
class LazyComputed<P1, R>(private val initializer: (P1) -> R) {
    private val _value = atomic<R?>(null)
    private var hasInitialized = false

    fun value(p1: P1): R {
        if (hasInitialized) return _value.value as R
        hasInitialized = true
        _value.update { initializer(p1) }
        return _value.value as R
    }

    fun reset() {
        hasInitialized = false
        _value.update { null }
    }
}
