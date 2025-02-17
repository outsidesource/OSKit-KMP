package com.outsidesource.oskitkmp.concurrency

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.*
import kotlin.jvm.JvmName

expect object KmpDispatchers {
    val IO: CoroutineDispatcher
    val Default: CoroutineDispatcher
    val Main: CoroutineDispatcher
    val Unconfined: CoroutineDispatcher
}

/**
 * Creates a deferred result that does not cancel the parent upon failure but instead returns an Outcome.
 * Best used with [awaitOutcome].
 */
fun <T> CoroutineScope.asyncOutcome(
    block: suspend CoroutineScope.() -> Outcome<T, Any>,
): Deferred<Outcome<T, Any>> = async {
    try {
        block()
    } catch (e: Throwable) {
        Outcome.Error(e)
    }
}

/**
 * Awaits a deferred and returns an outcome instead of throwing an error on cancellation.
 */
suspend fun <T> Deferred<T>.awaitOutcome(): Outcome<T, Any> = try {
    Outcome.Ok(await())
} catch (t: Throwable) {
    Outcome.Error(t)
}

/**
 * Awaits a deferred and returns an outcome instead of throwing an error on cancellation.
 * Best used with [asyncOutcome].
 */
@JvmName("awaitDeferredOutcome")
suspend fun <T> Deferred<Outcome<T, Any>>.awaitOutcome(): Outcome<T, Any> = try {
    await()
} catch (t: Throwable) {
    Outcome.Error(t)
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

suspend fun <T, E> withTimeoutOrOutcome(
    timeoutMillis: Long,
    timeoutError: E,
    block: suspend CoroutineScope.() -> Outcome<T, E>,
): Outcome<T, E> = withTimeoutOrNull(timeoutMillis, block) ?: Outcome.Error(timeoutError)

suspend fun <T> withTimeoutOrOutcome(
    timeoutMillis: Long,
    block: suspend CoroutineScope.() -> Outcome<T, Any>,
): Outcome<T, Any> = try {
    withTimeout(timeoutMillis, block)
} catch (e: Exception) {
    Outcome.Error(e)
}
