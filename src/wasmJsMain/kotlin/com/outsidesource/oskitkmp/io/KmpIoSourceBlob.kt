@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.filesystem.Blob
import com.outsidesource.oskitkmp.lib.toArrayBuffer
import org.khronos.webgl.ArrayBuffer

private const val BLOB_READ_BUFFER_SIZE = 64 * 1024

/**
 * Reads the rest of the source as a [Blob], for browser APIs like FormData and fetch.
 *
 * Blob-backed sources, like those from [com.outsidesource.oskitkmp.filesystem.source], hand over a slice of their own
 * Blob, so large files are never copied into wasm memory. Other sources are read into a new Blob.
 */
suspend fun IKmpIoSource.toBlob(): Blob {
    if (this is WasmKmpIoSource) return remainingBlob()

    val parts = JsArray<ArrayBuffer>()
    val buffer = ByteArray(BLOB_READ_BUFFER_SIZE)
    while (true) {
        val bytesRead = read(buffer)
        if (bytesRead == -1) break
        if (bytesRead > 0) parts[parts.length] = buffer.toArrayBuffer(byteCount = bytesRead)
    }
    return Blob(parts)
}
