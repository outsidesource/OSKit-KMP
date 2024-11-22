package com.outsidesource.oskitkmp.lib

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

suspend fun <T : JsAny?> Promise<T>.kmpAwait() = suspendCoroutine<T> { continuation ->
    then {
        continuation.resume(it)
        it
    }
        .catch {
            continuation.resumeWithException(KmpJsException(it))
            it
        }
}

suspend fun <T : JsAny?> Promise<T>.kmpAwaitOutcome() = suspendCoroutine<Outcome<T, JsAny>> { continuation ->
    then {
        continuation.resume(Outcome.Ok(it))
        it
    }
        .catch {
            continuation.resume(Outcome.Error(it))
            it
        }
}

data class KmpJsException(val error: JsAny) : Throwable("JS Exception: $error")
