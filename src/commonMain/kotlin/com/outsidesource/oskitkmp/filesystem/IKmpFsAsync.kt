package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.*

interface IKmpFsAsyncSource : IKmpFsClosable {
    suspend fun read(sink: ByteArray, offset: Int = 0, byteCount: Int = sink.size): Int
    suspend fun readUtf8Line(sink: ByteArray = ByteArray(1024)): String?
    suspend fun readAll(): ByteArray
    override suspend fun close()
    suspend fun isExhausted(): Boolean

    suspend fun readAllUtf8(): String = readAll().decodeToString()
    suspend fun readByte(sink: ByteArray = ByteArray(1)): Byte = sink.apply { read(this) }[0]
    suspend fun readShort(sink: ByteArray = ByteArray(2)): Short = sink.apply { read(this) }.toShort()
    suspend fun readInt(sink: ByteArray = ByteArray(4)): Int = sink.apply { read(this) }.toInt()
    suspend fun readFloat(sink: ByteArray = ByteArray(4)): Float = sink.apply { read(this) }.toFloat()
    suspend fun readDouble(sink: ByteArray = ByteArray(8)): Double = sink.apply { read(this) }.toDouble()
    suspend fun readLong(sink: ByteArray = ByteArray(8)): Long = sink.apply { read(this) }.toLong()
    suspend fun readUtf8(byteCount: Int, sink: ByteArray = ByteArray(byteCount)): String =
        sink.apply { read(this) }.decodeToString()
}

interface IKmpFsAsyncSink : IKmpFsClosable {
    suspend fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size): IKmpFsAsyncSink
    suspend fun flush()
    override suspend fun close()

    suspend fun writeByte(value: Byte): IKmpFsAsyncSink = write(byteArrayOf(value))
    suspend fun writeShort(value: Short): IKmpFsAsyncSink = write(value.toBytes())
    suspend fun writeInt(value: Int): IKmpFsAsyncSink = write(value.toBytes())
    suspend fun writeFloat(value: Float): IKmpFsAsyncSink = write(value.toBytes())
    suspend fun writeDouble(value: Double): IKmpFsAsyncSink = write(value.toBytes())
    suspend fun writeLong(value: Long): IKmpFsAsyncSink = write(value.toBytes())
    suspend fun writeUtf8(value: String): IKmpFsAsyncSink = write(value.encodeToByteArray())
}

// TODO: Read returns -1 when exhausted. This will break all read helpers by returning a bogus value.
// Maybe make a private helper that throws an exception?

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
