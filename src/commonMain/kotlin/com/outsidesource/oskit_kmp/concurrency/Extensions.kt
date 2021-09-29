package com.outsidesource.oskit_kmp.concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FlowTimeoutCancellationException(timeout: Long) : CancellationException("Flow timed out after ${timeout}ms")

/**
 * Stops the flow when the time elapsed since collect start >= timeout
 * Throws a [FlowTimeoutCancellationException] if throwOnTimeout == true
 */
fun <T> Flow<T>.withTimeout(timeout: Long, throwOnTimeout: Boolean = false): Flow<T> = flow {
    try {
        coroutineScope {
            val job = launch {
                delay(timeout)
                this@coroutineScope.cancel(FlowTimeoutCancellationException(timeout))
            }

            collect {
                emit(it)
            }

            job.cancel()
        }
    } catch (e: FlowTimeoutCancellationException) {
        if (throwOnTimeout) throw e
    }
}

/**
 * Run a block after a timeout/delay
 */
suspend fun withDelay(delayInMillis: Long, block: suspend () -> Any) = coroutineScope {
    launch {
        delay(delayInMillis)
        block()
    }
}