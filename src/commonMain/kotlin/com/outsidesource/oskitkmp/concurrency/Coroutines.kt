package com.outsidesource.oskitkmp.concurrency

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Creates a deferred result that does not cancel the parent upon failure but instead returns an Outcome.
 * Best used with [awaitOutcome].
 */
fun <T> CoroutineScope.asyncOutcome(
    block: suspend CoroutineScope.() -> Outcome<T, Any>
): Deferred<Outcome<T, Any>> = async {
    try {
        block()
    } catch (e: Throwable) {
        Outcome.Error(e)
    }
}

/**
 * Awaits a deferred and returns an outcome instead of throwing an error on cancellation.
 * Best used with [asyncOutcome].
 */
suspend fun <T> Deferred<Outcome<T, Any>>.awaitOutcome(): Outcome<T, Any> = try {
    await()
} catch (e: Throwable) {
    Outcome.Error(e)
}
