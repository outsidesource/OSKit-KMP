package com.outsidesource.oskitkmp.filesystem

import okio.Sink
import okio.buffer

class OkIoKmpFsSink(sink: Sink) : IKmpFsSink {
    private val buffer = sink.buffer()
    override suspend fun write(source: ByteArray, sourceOffset: Int, byteCount: Int): IKmpFsSink =
        buffer.write(source, sourceOffset, byteCount).let { return@let this }
    override suspend fun flush() = buffer.flush()
    override suspend fun close() = buffer.close()
}
