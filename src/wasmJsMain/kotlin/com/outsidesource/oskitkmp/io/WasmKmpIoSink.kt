@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.concurrency.kmpAwait
import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.filesystem.FileSystemWritableFileStream
import com.outsidesource.oskitkmp.filesystem.writeOptions
import com.outsidesource.oskitkmp.lib.toArrayBuffer

internal class WasmKmpIoSink(
    private val writableFileStream: FileSystemWritableFileStream,
) : IKmpIoSink {

    private var isClosed = false

    override suspend fun write(
        source: ByteArray,
        sourceOffset: Int,
        byteCount: Int,
    ): IKmpIoSink {
        check(!isClosed) { "closed" }
        val data = source.toArrayBuffer(startIndex = sourceOffset, byteCount = byteCount)
        val options = writeOptions(type = "write", data = data)
        writableFileStream.write(options).kmpAwait()
        return this
    }

    override suspend fun flush() { /* Noop in WASM */ }

    override suspend fun close() {
        writableFileStream.close().kmpAwaitOutcome()
        isClosed = true
    }
}
