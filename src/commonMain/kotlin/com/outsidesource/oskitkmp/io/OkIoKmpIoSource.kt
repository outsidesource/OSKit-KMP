package com.outsidesource.oskitkmp.io

import okio.Source
import okio.buffer

class OkIoKmpIoSource(source: Source) : IKmpIoSource {
    private val buffer = source.buffer()

    override suspend fun require(byteCount: Long) = buffer.require(byteCount)
    override suspend fun read(sink: ByteArray, sinkOffset: Int, byteCount: Int): Int = buffer.read(
        sink,
        sinkOffset,
        byteCount,
    )
    override suspend fun readUtf8Line(sink: ByteArray): String? = buffer.readUtf8Line()
    override suspend fun readRemaining(): ByteArray = buffer.readByteArray()
    override suspend fun close() = buffer.close()
    override suspend fun isExhausted(): Boolean = buffer.exhausted()
}
