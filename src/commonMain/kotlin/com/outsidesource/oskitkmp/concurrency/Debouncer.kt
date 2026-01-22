package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A coroutine based debouncer
 *
 * ```
 * val debouncer = Debouncer(200)
 *
 * for (i in 0..100) {
 *      debouncer.emit { println("Hello!") }
 * }
 * ```
 *
 * @param timeoutMillis How long to wait after the last emit before running the action
 * @param maxWaitMillis The maximum time to wait before running the action. This works similar to a throttle.
 * @param scope The coroutine scope to run the action in
 */
@OptIn(ExperimentalTime::class)
class Debouncer(
    private val timeoutMillis: Int,
    private val maxWaitMillis: Int = -1,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job()),
) {
    private var job: Job? = null
    private var lastEmit: Instant = Clock.System.now()
    private val lock = reentrantLock()

    /**
     * Schedules [func] to be run after the debounce [timeoutMillis] or when [maxWaitMillis] has elapsed
     * [isLastEmit] denotes if the func is running after the delay
     */
    fun emit(func: suspend (isLastEmit: Boolean) -> Unit): Unit = lock.withLock {
        job?.cancel()

        if (maxWaitMillis < 0) {
            lastEmit = Clock.System.now()
        } else if ((Clock.System.now() - lastEmit).inWholeMilliseconds >= maxWaitMillis) {
            lastEmit = Clock.System.now()
            scope.launch { func(false) }
            return
        }

        job = scope.launch {
            delay(timeoutMillis.toLong())
            func(true)
        }
    }
}
