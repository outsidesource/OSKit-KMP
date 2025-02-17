package com.outsidesource.oskitkmp.io

import okio.Source
import okio.buffer

class OkIoKmpIoSource(source: Source) : IKmpIoSource {
    private val buffer = source.buffer()

    override suspend fun require(byteCount: Long) = buffer.require(byteCount)
    override suspend fun read(buffer: ByteArray, bufferOffset: Int, byteCount: Int): Int =
        this@OkIoKmpIoSource.buffer.read(buffer, bufferOffset, byteCount)
    override suspend fun readUtf8Line(buffer: ByteArray): String? = this@OkIoKmpIoSource.buffer.readUtf8Line()
    override suspend fun readAll(): ByteArray = buffer.readByteArray()
    override suspend fun close() = buffer.close()
    override suspend fun isExhausted(): Boolean = buffer.exhausted()
}
