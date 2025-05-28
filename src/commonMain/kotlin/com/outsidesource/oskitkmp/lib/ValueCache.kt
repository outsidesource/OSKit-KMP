package com.outsidesource.oskitkmp.lib

import kotlinx.atomicfu.atomic

/**
 * [ValueCache] caches a value based on dependencies. Any time a dependency changes, the value is recomputed. If
 * the dependencies do not change, the cached value will be returned.
 *
 * ```
 * val myValueCache = ValueCache()
 *
 * fun main() {
 *     val result = myValueCache(dependency1) { /* Compute your value */ }
 * }
 * ```
 */
class ValueCache<T> {
    companion object {
        private val UNINITIALIZED = Any()
    }
    private var storedDependencies: Array<out Any?> = emptyArray()
    private var value: Any? = UNINITIALIZED

    operator fun invoke(
        vararg dependencies: Any?,
        compute: () -> T,
    ): T {
        if (value === UNINITIALIZED || !storedDependencies.contentEquals(dependencies)) {
            storedDependencies = dependencies.copyOf()
            value = compute()
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    fun reset() {
        storedDependencies = emptyArray()
        value = UNINITIALIZED
    }
}

/**
 * [ConcurrentValueCache] is a thread-safe [ValueCache]. [ConcurrentValueCache] caches a value based on dependencies.
 * Any time a dependency changes, the value is recomputed. If the dependencies do not change, the cached value will be
 * returned.
 *
 * ```
 * val myValueCache = ValueCache()
 *
 * fun main() {
 *     val result = myValueCache(dependency1) { /* Compute your value */ }
 * }
 * ```
 */
class ConcurrentValueCache<T> {
    companion object {
        private val UNINITIALIZED = Any()
    }
    private val cache = atomic<Pair<Array<out Any?>, Any?>>(emptyArray<Any?>() to UNINITIALIZED)

    operator fun invoke(
        vararg dependencies: Any?,
        compute: () -> T,
    ): T {
        val (currentDeps, currentValue) = cache.value

        if (currentValue !== UNINITIALIZED && currentDeps.contentEquals(dependencies)) {
            @Suppress("UNCHECKED_CAST")
            return currentValue as T
        }

        val newValue = compute()
        cache.value = dependencies.copyOf() to newValue

        return newValue
    }

    fun reset() {
        cache.value = emptyArray<Any?>() to UNINITIALIZED
    }
}
