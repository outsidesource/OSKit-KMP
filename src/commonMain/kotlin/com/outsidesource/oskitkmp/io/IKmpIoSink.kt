package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.lib.toBytes
import com.outsidesource.oskitkmp.lib.toBytesLe

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
