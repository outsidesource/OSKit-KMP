package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.concurrency.kmpAwait
import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.filesystem.Blob
import com.outsidesource.oskitkmp.lib.copyInto
import com.outsidesource.oskitkmp.lib.toByteArray
import com.outsidesource.oskitkmp.lib.toUint8Array
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import org.w3c.files.File

internal class WasmKmpIoSource(file: File) : IKmpIoSource {

    @Suppress("CAST_NEVER_SUCCEEDS")
    private val blob = file as Blob
    private var position: Long = 0L
    private val size: Long = blob.size.toDouble().toLong()
    private var isClosed: Boolean = false
    private val sb = StringBuilder()

    override suspend fun require(byteCount: Long) {
        if (byteCount > size - position) throw KmpIoError.Eof
    }

    override suspend fun read(buffer: ByteArray, bufferOffset: Int, byteCount: Int): Int {
        if (position >= size) return -1
        check(!isClosed) { "closed" }
        val end = minOf(position + byteCount, size)
        val slice = blob.slice(start = position.toDouble().toJsNumber(), end = end.toDouble().toJsNumber())
        slice.arrayBuffer().kmpAwait().toUint8Array().copyInto(buffer, bufferOffset)
        val read = (end - position)
        position += read
        return read.toInt()
    }

    override suspend fun readAll(): ByteArray {
        val bytes = blob.slice(start = position.toDouble().toJsNumber(), end = blob.size.toDouble().toJsNumber())
            .arrayBuffer()
            .kmpAwaitOutcome()
            .unwrapOrReturn { return ByteArray(0) }
            .toByteArray()
        position = size
        return bytes
    }

    override suspend fun readUtf8Line(buffer: ByteArray): String? =
        commonReadUtf8Line(buffer, sb, position) { position = it }

    override suspend fun close() { /* Noop in WASM */ }
    override suspend fun isExhausted(): Boolean = position >= size
}
