@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.lib

import com.outsidesource.oskitkmp.outcome.Outcome
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

@Suppress("UNCHECKED_CAST")
fun <T : JsAny> jsTryOutcome(block: () -> T): Outcome<T, Any> {
    val result = jsTry(block = block)
    result.error?.let { return Outcome.Error(it) }
    return Outcome.Ok(result.result as T)
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

/*
 * Byte copies between JS and Kotlin go through wasm linear memory. Indexing a Uint8Array from Kotlin is a JS call per
 * byte, which blocks the main thread for hundreds of milliseconds on a few MB. Linear memory keeps the per-byte loop
 * inside wasm and leaves JS a single bulk copy.
 */

fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Uint8Array(this)
    return ByteArray(source.length).also { source.copyInto(it) }
}

fun ArrayBuffer.toUint8Array(): Uint8Array = Uint8Array(this)

@OptIn(UnsafeWasmMemoryApi::class)
fun ByteArray.toArrayBuffer(startIndex: Int = 0, byteCount: Int = size - startIndex): ArrayBuffer =
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(byteCount)
        for (i in 0 until byteCount) (pointer + i).storeByte(this[startIndex + i])
        copyFromWasmMemory(wasmMemory, pointer.address.toInt(), byteCount)
    }

@OptIn(UnsafeWasmMemoryApi::class)
fun Uint8Array.copyInto(
    destination: ByteArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = length,
) {
    val byteCount = endIndex - startIndex
    if (byteCount <= 0) return

    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(byteCount)
        copyIntoWasmMemory(wasmMemory, pointer.address.toInt(), this, startIndex, endIndex)
        for (i in 0 until byteCount) destination[destinationOffset + i] = (pointer + i).loadByte()
    }
}

@Suppress("UnusedParameter")
private fun copyFromWasmMemory(memory: WebAssembly.Memory, address: Int, byteCount: Int): ArrayBuffer =
    js("memory.buffer.slice(address, address + byteCount)")

@Suppress("UnusedParameter")
private fun copyIntoWasmMemory(
    memory: WebAssembly.Memory,
    address: Int,
    source: Uint8Array,
    startIndex: Int,
    endIndex: Int,
): Unit = js("new Uint8Array(memory.buffer).set(source.subarray(startIndex, endIndex), address)")

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
