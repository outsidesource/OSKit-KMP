package com.outsidesource.oskitkmp.io

import okio.Sink
import okio.buffer

class OkIoKmpIoSink(sink: Sink) : IKmpIoSink {
    private val buffer = sink.buffer()
    override suspend fun write(source: ByteArray, sourceOffset: Int, byteCount: Int): IKmpIoSink =
        buffer.write(source, sourceOffset, byteCount).let { return@let this }
    override suspend fun flush() = buffer.flush()
    override suspend fun close() = buffer.close()
}
