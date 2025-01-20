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

interface IKmpIoSource : IKmpIoClosable {
    suspend fun read(sink: ByteArray, sinkOffset: Int = 0, byteCount: Int = sink.size): Int
    suspend fun readRemaining(): ByteArray
    suspend fun readUtf8Line(sink: ByteArray = ByteArray(1024)): String?
    override suspend fun close()
    suspend fun isExhausted(): Boolean
    suspend fun require(byteCount: Long)

    suspend fun readByte(sink: ByteArray = ByteArray(1)): Byte = sink.apply { checkedRead(1, this) }[0]
    suspend fun readShort(sink: ByteArray = ByteArray(2)): Short = sink.apply { checkedRead(2, this) }.toShort()
    suspend fun readShortLe(sink: ByteArray = ByteArray(2)): Short = sink.apply { checkedRead(2, this) }.toShortLe()
    suspend fun readInt(sink: ByteArray = ByteArray(4)): Int = sink.apply { checkedRead(4, this) }.toInt()
    suspend fun readIntLe(sink: ByteArray = ByteArray(4)): Int = sink.apply { checkedRead(4, this) }.toIntLe()
    suspend fun readFloat(sink: ByteArray = ByteArray(4)): Float = sink.apply { checkedRead(4, this) }.toFloat()
    suspend fun readFloatLe(sink: ByteArray = ByteArray(4)): Float = sink.apply { checkedRead(4, this) }.toFloatLe()
    suspend fun readDouble(sink: ByteArray = ByteArray(8)): Double = sink.apply { checkedRead(8, this) }.toDouble()
    suspend fun readDoubleLe(sink: ByteArray = ByteArray(8)): Double = sink.apply { checkedRead(8, this) }.toDoubleLe()
    suspend fun readLong(sink: ByteArray = ByteArray(8)): Long = sink.apply { checkedRead(8, this) }.toLong()
    suspend fun readLongLe(sink: ByteArray = ByteArray(8)): Long = sink.apply { checkedRead(8, this) }.toLongLe()
    suspend fun readUtf8(byteCount: Int, sink: ByteArray = ByteArray(byteCount)): String =
        sink.apply { checkedRead(byteCount.toLong(), this) }.decodeToString()
    suspend fun readAllUtf8(): String = readRemaining().decodeToString()

    suspend fun readAll(sink: IKmpIoSink, bufferSize: Int = 16384): Long {
        require(bufferSize > 0)
        val buffer = ByteArray(bufferSize)
        var totalBytesRead = 0L

        while (!isExhausted()) {
            val bytesRead = read(buffer)
            if (bytesRead == -1) return totalBytesRead
            totalBytesRead += bytesRead
        }

        return totalBytesRead
    }
}


private suspend fun IKmpIoSource.checkedRead(byteCount: Long, sink: ByteArray): Int {
    require(byteCount)
    return read(sink)
}

internal suspend inline fun IKmpIoSource.commonReadUtf8Line(
    sink: ByteArray,
    sb: StringBuilder,
    position: Long,
    updatePosition: (Long) -> Unit,
): String? {
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
            updatePosition(initialPosition + index + 1)
            val skip = if (index > 0 && sink[index - 1] == carriageReturn) 1 else 0
            sb.append(sink.decodeToString(0, index - skip))
            break
        }
    }
    return sb.toString()
}

private val newline = '\n'.code.toByte()
private val carriageReturn = '\r'.code.toByte()