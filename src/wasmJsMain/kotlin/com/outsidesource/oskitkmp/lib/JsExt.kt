package com.outsidesource.oskitkmp.lib

fun List<String>.toJsArray(): JsArray<JsString> = JsArray<JsString>().apply {
    this@toJsArray.forEachIndexed { i, string -> set(i, string.toJsString()) }
}
