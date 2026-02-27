@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.lib

external object JSON {
    fun stringify(data: JsAny): String
    fun parse(text: String): JsAny?
}
