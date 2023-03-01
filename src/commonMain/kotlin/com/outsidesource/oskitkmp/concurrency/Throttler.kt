package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class Throttler(
    private val timeoutMillis: Int,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) {
    private var lastEmit: Instant = Clock.System.now()
    private val lock = reentrantLock()

    fun emit(func: suspend () -> Unit) = lock.withLock {
        if ((Clock.System.now() - lastEmit).inWholeMilliseconds < timeoutMillis) return@withLock
        lastEmit = Clock.System.now()
        scope.launch { func() }
    }
}
