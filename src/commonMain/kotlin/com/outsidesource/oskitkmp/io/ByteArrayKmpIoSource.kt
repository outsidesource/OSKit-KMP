package com.outsidesource.oskitkmp.io

fun ByteArray.toKmpIoSource(): IKmpIoSource = ByteArrayKmpIoSource(this)

internal class ByteArrayKmpIoSource(private val bytes: ByteArray) : IKmpIoSource {
    private var position = 0
    private val sb = StringBuilder()

    override suspend fun read(buffer: ByteArray, bufferOffset: Int, byteCount: Int): Int {
        if (position >= bytes.size) return -1
        val end = minOf(position + byteCount, bytes.size)
        bytes.copyOfRange(position, end).copyInto(buffer, bufferOffset)
        val read = (end - position)
        position += read
        return read.toInt()
    }

    override suspend fun readAll(): ByteArray =
        bytes.copyOfRange(position, bytes.size).also { position = bytes.size }

    override suspend fun readUtf8Line(buffer: ByteArray): String? =
        commonReadUtf8Line(buffer, sb, position.toLong()) { position = it.toInt() }

    override suspend fun close() { /* Noop */ }

    override suspend fun isExhausted(): Boolean = position >= bytes.size

    override suspend fun require(byteCount: Long) {
        if (byteCount > bytes.size - position) throw KmpIoError.Eof
    }
}
