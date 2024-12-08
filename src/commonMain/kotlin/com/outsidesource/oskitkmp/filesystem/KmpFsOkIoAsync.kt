package com.outsidesource.oskitkmp.filesystem

import okio.Sink
import okio.Source
import okio.buffer

class KmpFsOkIoAsyncSource(source: Source) : IKmpFsAsyncSource {
    private val buffer = source.buffer()

    override suspend fun read(sink: ByteArray, offset: Int, byteCount: Int): Int = buffer.read(sink, offset, byteCount)
    override suspend fun readUtf8Line(sink: ByteArray): String? = buffer.readUtf8Line()
    override suspend fun readAll(): ByteArray = buffer.readByteArray()
    override suspend fun close() = buffer.close()
    override suspend fun isExhausted(): Boolean = buffer.exhausted()
}

class KmpFsOkIoAsyncSink(sink: Sink) : IKmpFsAsyncSink {
    private val buffer = sink.buffer()
    override suspend fun write(source: ByteArray, offset: Int, byteCount: Int): IKmpFsAsyncSink =
        buffer.write(source, offset, byteCount).let { return@let this }
    override suspend fun flush() = buffer.flush()
    override suspend fun close() = buffer.close()
}
