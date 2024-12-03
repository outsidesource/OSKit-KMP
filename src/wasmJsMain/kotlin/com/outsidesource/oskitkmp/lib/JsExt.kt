package com.outsidesource.oskitkmp.lib

import com.outsidesource.oskitkmp.outcome.Outcome

@Suppress("UNCHECKED_CAST")
fun <T : JsAny> jsTryOutcome(block: () -> T): Outcome<T, Any> {
    val result = jsTry(block = block)
    result.error?.let { return Outcome.Error(it) }
    return Outcome.Ok(result as T)
}

fun <T : JsAny?> jsTry(block: () -> T): JsResult<T> = js(
    """{
        try {
            return { result: block() }
        } catch (e) {
            return { error: e }
        }
    }""",
)

external interface JsResult<T : JsAny?> : JsAny {
    val error: JsAny?
    val result: T?
}
