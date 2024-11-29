package com.outsidesource.oskitkmp.lib

import com.outsidesource.oskitkmp.outcome.Outcome

fun List<String>.toJsArray(): JsArray<JsString> = JsArray<JsString>().apply {
    this@toJsArray.forEachIndexed { i, string -> set(i, string.toJsString()) }
}

fun JsArray<JsString>.toSet(): Set<String> = buildSet {
    apply {
        for (i in 0 until length) {
            get(i)?.let { add(it.toString()) }
        }
    }
}

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
