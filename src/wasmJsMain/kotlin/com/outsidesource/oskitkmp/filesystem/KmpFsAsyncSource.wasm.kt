package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwait
import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.lib.copyInto
import com.outsidesource.oskitkmp.lib.toByteArray
import com.outsidesource.oskitkmp.lib.toUint8Array
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import org.w3c.files.File

class KmpFsAsyncSource(file: File) : IKmpFsAsyncSource {

    @Suppress("CAST_NEVER_SUCCEEDS")
    private val blob = file as Blob
    private var position: Long = 0L
    private val size: Long = blob.size.toDouble().toLong()
    private val newline = '\n'.code.toByte()

    override suspend fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        if (position >= size) return -1
        val end = minOf(position + byteCount, size)
        val slice = blob.slice(start = position.toDouble().toJsNumber(), end = end.toDouble().toJsNumber())
        slice.arrayBuffer().kmpAwait().toUint8Array().copyInto(sink, offset)
        val read = (end - position)
        position += read
        return read.toInt()
    }

    override suspend fun readUtf8Line(sink: ByteArray): String? {
        val bytesRead = read(sink)
        if (bytesRead == -1) return null
        val index = sink.indexOf(newline)
        if (position >= size || index == -1) return sink.decodeToString(0, bytesRead)
        position = position - (sink.size - index - 1) // -1 for consuming '\n' character
        return sink.decodeToString(0, index)
    }

    override suspend fun readAll(): ByteArray {
        val bytes = blob.arrayBuffer().kmpAwaitOutcome().unwrapOrReturn { return ByteArray(0) }.toByteArray()
        position = size
        return bytes
    }

    override suspend fun close() = blob.close()
    override suspend fun isExhausted(): Boolean = position >= size
}
