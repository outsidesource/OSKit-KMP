package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.annotation.ExperimentalOsKitApi

@ExperimentalOsKitApi
fun ByteArray.toKmpFsSink() : IKmpFsSink = ByteArrayKmpFsSink(this)

internal class ByteArrayKmpFsSink(private val bytes: ByteArray) : IKmpFsSink {

    private var position = 0
    private var isClosed = false

    override suspend fun write(
        source: ByteArray,
        sourceOffset: Int,
        byteCount: Int,
    ): IKmpFsSink {
        check(!isClosed) { "closed" }
        if (position + byteCount >= bytes.size) throw EofError()
        source.copyInto(bytes, position, sourceOffset, sourceOffset + byteCount)
        position += byteCount
        return this
    }

    override suspend fun flush() { /* Noop */ }

    override suspend fun close() {
        isClosed = true
    }
}