package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.lib.toDouble
import com.outsidesource.oskitkmp.lib.toDoubleLe
import com.outsidesource.oskitkmp.lib.toFloat
import com.outsidesource.oskitkmp.lib.toFloatLe
import com.outsidesource.oskitkmp.lib.toInt
import com.outsidesource.oskitkmp.lib.toIntLe
import com.outsidesource.oskitkmp.lib.toLong
import com.outsidesource.oskitkmp.lib.toLongLe
import com.outsidesource.oskitkmp.lib.toShort
import com.outsidesource.oskitkmp.lib.toShortLe

/**
 * Supplies a stream of bytes that allows easy reading of multiple data types
 *
 * All methods have a possibility of throwing exceptions.
 */
interface IKmpIoSource : IKmpIoClosable {
    /**
     * Reads bytes into the provided [buffer]
     *
     * @param buffer The destination for the read bytes
     * @param bufferOffset The start offset to place read bytes in the sink
     * @param byteCount The amount of bytes to read from the source
     *
     * @return The amount of bytes read
     */
    suspend fun read(buffer: ByteArray, bufferOffset: Int = 0, byteCount: Int = buffer.size): Int

    /**
     * Reads all remaining bytes in the source and returns them as a ByteArray
     */
    suspend fun readAll(): ByteArray

    /**
     * Reads a single Utf8 encoded line from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readUtf8Line(buffer: ByteArray = ByteArray(1024)): String?

    /**
     * Closes the source. Any further operation will return an error.
     */
    override suspend fun close()

    /**
     * Returns [true] if the source has no more bytes
     */
    suspend fun isExhausted(): Boolean

    /**
     * Returns when the buffer contains at least [byteCount] bytes. Throws a [KmpIoError.Eof] if the source is exhausted before the required bytes can be read.
     */
    suspend fun require(byteCount: Long)

    /**
     * Reads a single byte from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readByte(buffer: ByteArray = ByteArray(1)): Byte = buffer.apply { checkedRead(1, this) }[0]

    /**
     * Reads a short from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readShort(buffer: ByteArray = ByteArray(2)): Short = buffer.apply { checkedRead(2, this) }.toShort()

    /**
     * Reads a short in little-endian order from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readShortLe(buffer: ByteArray = ByteArray(2)): Short = buffer.apply { checkedRead(2, this) }.toShortLe()

    /**
     * Reads an int from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readInt(buffer: ByteArray = ByteArray(4)): Int = buffer.apply { checkedRead(4, this) }.toInt()

    /**
     * Reads an int in little-endian order from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readIntLe(buffer: ByteArray = ByteArray(4)): Int = buffer.apply { checkedRead(4, this) }.toIntLe()

    /**
     * Reads a float from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readFloat(buffer: ByteArray = ByteArray(4)): Float = buffer.apply { checkedRead(4, this) }.toFloat()

    /**
     * Reads a float in little-endian order from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readFloatLe(buffer: ByteArray = ByteArray(4)): Float = buffer.apply { checkedRead(4, this) }.toFloatLe()

    /**
     * Reads a double from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readDouble(buffer: ByteArray = ByteArray(8)): Double = buffer.apply { checkedRead(8, this) }.toDouble()

    /**
     * Reads a double in little-endian order from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readDoubleLe(buffer: ByteArray = ByteArray(8)): Double =
        buffer.apply { checkedRead(8, this) }.toDoubleLe()

    /**
     * Reads a long from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readLong(buffer: ByteArray = ByteArray(8)): Long = buffer.apply { checkedRead(8, this) }.toLong()

    /**
     * Reads a long in little-endian order from the source
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readLongLe(buffer: ByteArray = ByteArray(8)): Long = buffer.apply { checkedRead(8, this) }.toLongLe()

    /**
     * Reads a UTF-8 encoded string from the source
     *
     * @param bytesCount The amount of bytes to read
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readUtf8(byteCount: Int, buffer: ByteArray = ByteArray(byteCount)): String =
        buffer.apply { checkedRead(byteCount.toLong(), this) }.decodeToString()

    /**
     * Reads all remaining bytes from the source and returns them as a UTF-8 encoded string
     */
    suspend fun readAllUtf8(): String = readAll().decodeToString()

    /**
     * Reads all remaining bytes from the source into provided [sink]
     *
     * @param buffer Allows calling code to pool buffers and pass them in for improved performance.
     */
    suspend fun readAll(sink: IKmpIoSink, buffer: ByteArray = ByteArray(16384)): Long {
        var totalBytesRead = 0L

        while (!isExhausted()) {
            val bytesRead = read(buffer)
            if (bytesRead == -1) return totalBytesRead
            sink.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
        }

        return totalBytesRead
    }
}

private suspend fun IKmpIoSource.checkedRead(byteCount: Long, buffer: ByteArray): Int {
    require(byteCount)
    return read(buffer)
}

internal suspend inline fun IKmpIoSource.commonReadUtf8Line(
    buffer: ByteArray,
    sb: StringBuilder,
    position: Long,
    updatePosition: (Long) -> Unit,
): String? {
    sb.clear()
    while (true) {
        val initialPosition = position
        val bytesRead = read(buffer)
        if (bytesRead == -1) return null
        val index = buffer.indexOf(newline)
        if (index == -1) {
            val skip = if (bytesRead > 0 && buffer[bytesRead - 1] == carriageReturn) 1 else 0
            sb.append(buffer.decodeToString(0, bytesRead - skip))
            if (isExhausted()) break
        } else {
            updatePosition(initialPosition + index + 1)
            val skip = if (index > 0 && buffer[index - 1] == carriageReturn) 1 else 0
            sb.append(buffer.decodeToString(0, index - skip))
            break
        }
    }
    return sb.toString()
}

private val newline = '\n'.code.toByte()
private val carriageReturn = '\r'.code.toByte()
