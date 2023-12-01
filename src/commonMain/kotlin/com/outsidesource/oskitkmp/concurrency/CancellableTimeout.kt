package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.*

class CancellableTimeout(
    private val timeoutMillis: Int,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job()),
    private val onTimeout: suspend () -> Unit,
) {
    private var job: Job? = null
    private val lock = reentrantLock()

    fun start() = reset()

    fun reset() = lock.withLock {
        job?.cancel()

        job = scope.launch {
            delay(timeoutMillis.toLong())
            onTimeout()
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
