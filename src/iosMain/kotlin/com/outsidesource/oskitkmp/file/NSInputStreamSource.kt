package com.outsidesource.oskitkmp.file

import kotlinx.cinterop.*
import okio.Buffer
import okio.IOException
import okio.Source
import okio.Timeout
import platform.Foundation.NSInputStream
import platform.Foundation.NSStreamStatusClosed
import platform.Foundation.NSStreamStatusNotOpen
import platform.posix.uint8_tVar

fun NSInputStream.source(): Source = NSInputStreamSource(this)

private open class NSInputStreamSource(
    private val input: NSInputStream,
) : Source {

    private val buffer = ByteArray(8192)

    init {
        if (input.streamStatus == NSStreamStatusNotOpen) input.open()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
        if (input.streamStatus == NSStreamStatusClosed) throw IOException("Stream Closed")

        if (byteCount == 0L) return 0L
        require(byteCount >= 0L) { "byteCount < 0: $byteCount" }

        val maxToCopy = minOf(byteCount, buffer.size.toLong())
        val bytesRead = buffer.usePinned {
            val bytes = it.addressOf(0).reinterpret<uint8_tVar>()
            input.read(bytes, maxToCopy.convert())
        }

        if (bytesRead < 0L) throw IOException(input.streamError?.localizedDescription ?: "Unknown error")
        if (bytesRead == 0L) return -1

        sink.write(buffer, 0, bytesRead.toInt())

        return bytesRead
    }

    override fun close() = input.close()

    override fun timeout() = Timeout.NONE

    override fun toString() = "source($input)"
}
