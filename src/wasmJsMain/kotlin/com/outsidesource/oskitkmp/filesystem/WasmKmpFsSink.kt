package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.lib.toArrayBuffer
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

internal class WasmKmpFsSink(
    private val writableFileStream: FileSystemWritableFileStream,
) : IKmpFsSink {

    private var isClosed = false

    override suspend fun write(
        source: ByteArray,
        sourceOffset: Int,
        byteCount: Int,
    ): IKmpFsSink {
        check(!isClosed) { "closed" }
        val data = source.toArrayBuffer(startIndex = sourceOffset, byteCount = byteCount)
        val options = writeOptions(type = "write", data = data)
        writableFileStream.write(options).kmpAwaitOutcome().unwrapOrReturn { throw WriteError() }
        return this
    }

    override suspend fun flush() { /* Noop in WASM */ }

    override suspend fun close() {
        writableFileStream.close().kmpAwaitOutcome()
        isClosed = true
    }
}
