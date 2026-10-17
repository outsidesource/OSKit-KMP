package com.outsidesource.oskitkmp.io

import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.InputStream

private const val INPUT_STREAM_BUFFER_SIZE = 64 * 1024

/**
 * Reads the source as a Java [InputStream]
 *
 * Okio-backed sources, like those from [com.outsidesource.oskitkmp.filesystem.source], hand over Okio's own stream.
 * Other sources are read through a blocking adapter, so this should be called off the main thread.
 */
fun IKmpIoSource.inputStream(): InputStream {
    if (this is OkIoKmpIoSource) return buffer.inputStream()
    // Buffered so each blocking read covers many of the small reads Java streams tend to make
    return BufferedInputStream(KmpIoSourceInputStream(this), INPUT_STREAM_BUFFER_SIZE)
}

private class KmpIoSourceInputStream(private val source: IKmpIoSource) : InputStream() {
    override fun read(): Int {
        val byte = ByteArray(1)
        return if (read(byte, 0, 1) == -1) -1 else byte[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = runBlocking { source.read(b, off, len) }

    override fun close() = runBlocking { source.close() }
}
