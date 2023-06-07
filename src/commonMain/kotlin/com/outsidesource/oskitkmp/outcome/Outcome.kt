package com.outsidesource.oskitkmp.outcome

sealed class Outcome<out V, out E> {
    data class Ok<out V>(val value: V) : Outcome<V, Nothing>()
    data class Error<out E>(val error: E) : Outcome<Nothing, E>()

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <T, E : Throwable> tryBlock(block: () -> T): Outcome<T, E> = try {
            Ok(block())
        } catch (e: Throwable) {
            Error(e) as Error<E>
        }
    }
}

fun <V, E> Outcome<V, E>.getOrElse(default: V): V = when (this) {
    is Outcome.Ok -> this.value
    else -> default
}

fun <V, E> Outcome<V, E>.getOrNull(): V? = when (this) {
    is Outcome.Ok -> this.value
    else -> null
}
