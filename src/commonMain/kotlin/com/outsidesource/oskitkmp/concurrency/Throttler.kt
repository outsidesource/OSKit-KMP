package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * A coroutine-based throttler
 *
 * ```
 * val throttler = Throttler(200)
 *
 * for (i in 0..100) {
 *      delay(100)
 *      throttler.emit { println("Hello!") }
 * }
 * ```
 *
 * @param timeoutMillis The minimum time interval to wait before running the provided action
 * @param scope The coroutine scope to run the action in
 */
class Throttler(
    private val timeoutMillis: Int,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job()),
) {
    private var lastEmit: Instant = Clock.System.now()
    private val lock = reentrantLock()

    fun emit(func: suspend () -> Unit) = lock.withLock {
        if ((Clock.System.now() - lastEmit).inWholeMilliseconds < timeoutMillis) return@withLock
        lastEmit = Clock.System.now()
        scope.launch { func() }
    }
}
