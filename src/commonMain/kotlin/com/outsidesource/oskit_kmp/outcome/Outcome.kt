package com.outsidesource.oskit_kmp.outcome

import kotlin.Exception

sealed class Outcome<out T> {
    data class Ok<out T>(val value: T) : Outcome<T>()
    data class Error(val error: Any) : Outcome<Nothing>()

    companion object {
        inline fun <T> tryBlock(block: () -> T): Outcome<T> = try {
            Ok(block())
        } catch (e: Exception) {
            Error(e)
        }
    }
}
