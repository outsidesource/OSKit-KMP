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
    private val carriageReturn = '\r'.code.toByte()
    private val sb = StringBuilder()

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
        sb.clear()
        while (true) {
            val initialPosition = position
            val bytesRead = read(sink)
            if (bytesRead == -1) return null
            val index = sink.indexOf(newline)
            if (index == -1) {
                val skip = if (bytesRead > 0 && sink[bytesRead - 1] == carriageReturn) 1 else 0
                sb.append(sink.decodeToString(0, bytesRead - skip))
                if (isExhausted()) break
            } else {
                position = initialPosition + index + 1
                val skip = if (index > 0 && sink[index - 1] == carriageReturn) 1 else 0
                sb.append(sink.decodeToString(0, index - skip))
                break
            }
        }
        return sb.toString()
    }

    override suspend fun readAll(): ByteArray {
        val bytes = blob.arrayBuffer().kmpAwaitOutcome().unwrapOrReturn { return ByteArray(0) }.toByteArray()
        position = size
        return bytes
    }

    override suspend fun close() = blob.close()
    override suspend fun isExhausted(): Boolean = position >= size
}
