package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.annotation.ExperimentalOsKitApi
import com.outsidesource.oskitkmp.filesystem.KmpFsError

@ExperimentalOsKitApi
fun ByteArray.toKmpFsSource(): IKmpIoSource = ByteArrayKmpIoSource(this)

internal class ByteArrayKmpIoSource(private val bytes: ByteArray) : IKmpIoSource {
    private var position = 0

    private val newline = '\n'.code.toByte()
    private val carriageReturn = '\r'.code.toByte()
    private val sb = StringBuilder()

    override suspend fun read(sink: ByteArray, sinkOffset: Int, byteCount: Int): Int {
        if (position >= bytes.size) return -1
        val end = minOf(position + byteCount, bytes.size)
        bytes.copyOfRange(position, end).copyInto(sink, sinkOffset)
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
        position = bytes.size
        return bytes
    }

    override suspend fun close() { /* Noop */ }

    override suspend fun isExhausted(): Boolean = position >= bytes.size

    override suspend fun require(byteCount: Long) {
        if (byteCount > bytes.size - position) throw KmpFsError.EofError
    }
}
