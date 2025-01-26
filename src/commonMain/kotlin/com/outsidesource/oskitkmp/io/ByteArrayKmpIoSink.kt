package com.outsidesource.oskitkmp.io

fun ByteArray.toKmpIoSink(): IKmpIoSink = ByteArrayKmpIoSink(this)

internal class ByteArrayKmpIoSink(private val bytes: ByteArray) : IKmpIoSink {

    private var position = 0
    private var isClosed = false

    override suspend fun write(
        source: ByteArray,
        sourceOffset: Int,
        byteCount: Int,
    ): IKmpIoSink {
        check(!isClosed) { "closed" }
        if (position + byteCount > bytes.size) throw KmpIoError.Eof
        source.copyInto(bytes, position, sourceOffset, sourceOffset + byteCount)
        position += byteCount
        return this
    }

    override suspend fun flush() { /* Noop */ }

    override suspend fun close() {
        isClosed = true
    }
}
