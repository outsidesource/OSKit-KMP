package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.lib.toArrayBuffer
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

internal class WasmKmpFsSink(
    private val writableFileStream: FileSystemWritableFileStream,
) : IKmpFsSink {

    override suspend fun write(
        source: ByteArray,
        offset: Int,
        byteCount: Int,
    ): IKmpFsSink {
        val options = writeOptions(type = "write", data = source.toArrayBuffer())
        writableFileStream.write(options).kmpAwaitOutcome().unwrapOrReturn { throw WriteError() }
        return this
    }

    override suspend fun flush() { /* Noop in WASM */ }

    override suspend fun close() {
        writableFileStream.close().kmpAwaitOutcome()
    }
}
