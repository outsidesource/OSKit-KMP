package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.*

interface IKmpFsSource : IKmpFsClosable {
    suspend fun read(sink: ByteArray, sinkOffset: Int = 0, byteCount: Int = sink.size): Int
    suspend fun readUtf8Line(sink: ByteArray = ByteArray(1024)): String?
    suspend fun readAll(): ByteArray
    override suspend fun close()
    suspend fun isExhausted(): Boolean
    suspend fun require(byteCount: Long)

    suspend fun readAllUtf8(): String = readAll().decodeToString()
    suspend fun readByte(sink: ByteArray = ByteArray(1)): Byte = sink.apply { checkedRead(1, this) }[0]
    suspend fun readShort(sink: ByteArray = ByteArray(2)): Short = sink.apply { checkedRead(2, this) }.toShort()
    suspend fun readInt(sink: ByteArray = ByteArray(4)): Int = sink.apply { checkedRead(4, this) }.toInt()
    suspend fun readFloat(sink: ByteArray = ByteArray(4)): Float = sink.apply { checkedRead(4, this) }.toFloat()
    suspend fun readDouble(sink: ByteArray = ByteArray(8)): Double = sink.apply { checkedRead(8, this) }.toDouble()
    suspend fun readLong(sink: ByteArray = ByteArray(8)): Long = sink.apply { checkedRead(8, this) }.toLong()
    suspend fun readUtf8(byteCount: Int, sink: ByteArray = ByteArray(byteCount)): String =
        sink.apply { checkedRead(byteCount.toLong(), this) }.decodeToString()

    suspend fun readAll(sink: IKmpFsSink, bufferSize: Int = 16384): Long {
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

private suspend fun IKmpFsSource.checkedRead(byteCount: Long, sink: ByteArray): Int {
    require(byteCount)
    return read(sink)
}

interface IKmpFsSink : IKmpFsClosable {
    suspend fun write(source: ByteArray, sourceOffset: Int = 0, byteCount: Int = source.size): IKmpFsSink
    suspend fun flush()
    override suspend fun close()

    suspend fun writeByte(value: Byte): IKmpFsSink = write(byteArrayOf(value))
    suspend fun writeShort(value: Short): IKmpFsSink = write(value.toBytes())
    suspend fun writeInt(value: Int): IKmpFsSink = write(value.toBytes())
    suspend fun writeFloat(value: Float): IKmpFsSink = write(value.toBytes())
    suspend fun writeDouble(value: Double): IKmpFsSink = write(value.toBytes())
    suspend fun writeLong(value: Long): IKmpFsSink = write(value.toBytes())
    suspend fun writeUtf8(value: String): IKmpFsSink = write(value.encodeToByteArray())
    suspend fun writeAll(source: IKmpFsSource, bufferSize: Int = 16384): Long = source.readAll(this)
}

interface IKmpFsClosable {
    suspend fun close()
}

suspend inline fun <T : IKmpFsClosable, R> T.use(block: (T) -> R): R {
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

    if (thrown != null) throw thrown

    @Suppress("UNCHECKED_CAST")
    return result as R
}
