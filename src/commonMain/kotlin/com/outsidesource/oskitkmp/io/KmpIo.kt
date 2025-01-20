package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.lib.*
import com.outsidesource.oskitkmp.outcome.Outcome

interface IKmpIoSource : IKmpIoClosable {
    suspend fun read(sink: ByteArray, sinkOffset: Int = 0, byteCount: Int = sink.size): Int
    suspend fun readUtf8Line(sink: ByteArray = ByteArray(1024)): String?
    suspend fun readAll(): ByteArray
    override suspend fun close()
    suspend fun isExhausted(): Boolean
    suspend fun require(byteCount: Long)

    suspend fun readAllUtf8(): String = readAll().decodeToString()
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

interface IKmpIoSink : IKmpIoClosable {
    suspend fun write(source: ByteArray, sourceOffset: Int = 0, byteCount: Int = source.size): IKmpIoSink
    suspend fun flush()
    override suspend fun close()

    suspend fun writeByte(value: Byte): IKmpIoSink = write(byteArrayOf(value))
    suspend fun writeShort(value: Short): IKmpIoSink = write(value.toBytes())
    suspend fun writeShortLe(value: Short): IKmpIoSink = write(value.toBytesLe())
    suspend fun writeInt(value: Int): IKmpIoSink = write(value.toBytes())
    suspend fun writeIntLe(value: Int): IKmpIoSink = write(value.toBytesLe())
    suspend fun writeFloat(value: Float): IKmpIoSink = write(value.toBytes())
    suspend fun writeFloatLe(value: Float): IKmpIoSink = write(value.toBytesLe())
    suspend fun writeDouble(value: Double): IKmpIoSink = write(value.toBytes())
    suspend fun writeDoubleLe(value: Double): IKmpIoSink = write(value.toBytesLe())
    suspend fun writeLong(value: Long): IKmpIoSink = write(value.toBytes())
    suspend fun writeLongLe(value: Long): IKmpIoSink = write(value.toBytesLe())
    suspend fun writeUtf8(value: String): IKmpIoSink = write(value.encodeToByteArray())
    suspend fun writeAll(source: IKmpIoSource, bufferSize: Int = 16384): Long = source.readAll(this)
}

interface IKmpIoClosable {
    suspend fun close()
}

suspend inline fun <T : IKmpIoClosable, R> T.use(block: (T) -> R): Outcome<R, KmpIoError> {
    var thrown: Throwable? = null

    val result = try {
        block(this)
    } catch (t: Throwable) {
        thrown = t
        null
    } finally {
        try {
            close()
        } catch (t: Throwable) {
            if (thrown == null) thrown = t else thrown.addSuppressed(t)
        }
    }

    if (thrown != null) return Outcome.Error(KmpIoError.Unknown(thrown))

    @Suppress("UNCHECKED_CAST")
    return Outcome.Ok(result as R)
}

sealed class KmpIoError {
    data class Unknown(val t: Throwable) : KmpIoError()
}
