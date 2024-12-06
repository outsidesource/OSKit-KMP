package com.outsidesource.oskitkmp.filesystem

import kotlinx.cinterop.*
import okio.ArrayIndexOutOfBoundsException
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Timeout
import platform.Foundation.NSOutputStream
import platform.Foundation.NSStreamStatusClosed
import platform.Foundation.NSStreamStatusNotOpen
import platform.posix.uint8_tVar

fun NSOutputStream.sink(): Sink = OutputStreamSink(this)

private open class OutputStreamSink(
    private val out: NSOutputStream,
) : Sink {

    private val buffer = ByteArray(8192)

    init {
        if (out.streamStatus == NSStreamStatusNotOpen) out.open()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun write(source: Buffer, byteCount: Long) {
        if (out.streamStatus == NSStreamStatusClosed) throw IOException("Stream Closed")

        checkOffsetAndCount(source.size, 0, byteCount)
        var remaining = byteCount
        var pos = 0L

        while (remaining > 0) {
            val byteCountToCopy = minOf(remaining, buffer.size.toLong())
            pos += source.read(buffer, 0, byteCountToCopy.toInt())

            val bytesWritten = buffer.usePinned {
                val bytes = it.addressOf(0).reinterpret<uint8_tVar>()
                out.write(bytes, byteCountToCopy.convert())
            }

            if (bytesWritten < 0L) throw IOException(out.streamError?.localizedDescription ?: "Unknown error")
            if (bytesWritten == 0L) throw IOException("NSOutputStream reached capacity")

            remaining -= bytesWritten
        }
    }

    override fun flush() {
        // no-op
    }

    override fun close() = out.close()

    override fun timeout(): Timeout = Timeout.NONE

    override fun toString() = "RawSink($out)"

    private fun checkOffsetAndCount(size: Long, offset: Long, byteCount: Long) {
        if (offset or byteCount < 0 || offset > size || size - offset < byteCount) {
            throw ArrayIndexOutOfBoundsException("size=$size offset=$offset byteCount=$byteCount")
        }
    }
}
