package com.outsidesource.oskitkmp.lib

import com.outsidesource.oskitkmp.outcome.Outcome
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

external object JSON {
    fun stringify(data: JsAny): String
    fun parse(text: String): JsAny?
}

@Suppress("UNCHECKED_CAST")
fun <T : JsAny> jsTryOutcome(block: () -> T): Outcome<T, Any> {
    val result = jsTry(block = block)
    result.error?.let { return Outcome.Error(it) }
    return Outcome.Ok(result as T)
}

private fun <T : JsAny?> jsTry(block: () -> T): JsResult<T> = js(
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

fun ArrayBuffer.toByteArray(): ByteArray {
    val uint8Array = Uint8Array(this)
    return ByteArray(uint8Array.length) { uint8Array[it] }
}

fun ArrayBuffer.toUint8Array(): Uint8Array = Uint8Array(this)

fun ByteArray.toArrayBuffer(startIndex: Int = 0, byteCount: Int = size - startIndex): ArrayBuffer {
    val buffer = ArrayBuffer(byteCount)
    val array = Uint8Array(buffer)
    for (i in startIndex until size) array[i] = this[i]
    return buffer
}

fun Uint8Array.copyInto(
    destination: ByteArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = length,
) {
    for (i in startIndex until endIndex) { destination[destinationOffset + i] = this[i] }
}

/**
 * Converts a JsNumber to Long
 * Note: this potentially loses precision just like the int versions. JsNumbers are 64-bit floating values that can
 * support higher numbers with less precision than Kotlin Long
 */
fun JsNumber.toLong() = toDouble().toLong()

/**
 * Converts a Long to a JsNumber
 * Note: this potentially loses precision just like the int versions. JsNumbers are 64-bit floating values that can
 * support higher numbers with less precision than Kotlin Long
 */
fun Long.toJsNumber() = toDouble().toJsNumber()
