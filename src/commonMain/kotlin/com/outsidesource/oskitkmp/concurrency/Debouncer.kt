package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class Debouncer(
    private val timeoutMillis: Int,
    private val maxWaitMillis: Int = -1,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job()),
) {
    private var job: Job? = null
    private var lastEmit: Instant = Clock.System.now()
    private val lock = reentrantLock()

    fun emit(func: suspend () -> Unit) = lock.withLock {
        job?.cancel()

        if (maxWaitMillis < 0) {
            lastEmit = Clock.System.now()
        } else if ((Clock.System.now() - lastEmit).inWholeMilliseconds >= maxWaitMillis) {
            lastEmit = Clock.System.now()
            scope.launch { func() }
            return
        }

        job = scope.launch {
            delay(timeoutMillis.toLong())
            func()
        }
    }
}
