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

fun <V, E> Outcome<V, E>.unwrapOrDefault(default: V): V = when (this) {
    is Outcome.Ok -> this.value
    else -> default
}

fun <V, E> Outcome<V, E>.unwrapOrNull(): V? = when (this) {
    is Outcome.Ok -> this.value
    else -> null
}

inline fun <T, E> Outcome<T, E>.unwrapOrReturn(block: Outcome.Error<E>.() -> Nothing): T {
    when (this) {
        is Outcome.Ok -> return value
        is Outcome.Error -> block(this)
    }
}

inline fun <T, E> Outcome<T, E>.runOnOk(block: (T) -> Unit): Outcome<T, E> {
    if (this is Outcome.Ok) block(value)
    return this
}

inline fun <T, E> Outcome<T, E>.runOnError(block: (E) -> Unit): Outcome<T, E> {
    if (this is Outcome.Error) block(error)
    return this
}
