package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.lib.toBytes
import com.outsidesource.oskitkmp.lib.toBytesLe

/**
 * Receives a stream of bytes that allows easy writing of multiple data types
 *
 * All methods have a possibility of throwing exceptions.
 */
interface IKmpIoSink : IKmpIoClosable {

    /**
     * Writes [byteCount] bytes starting at [sourceOffset] from the [source] into the sink
     */
    suspend fun write(source: ByteArray, sourceOffset: Int = 0, byteCount: Int = source.size): IKmpIoSink

    /**
     * Flushes all written bytes to the destination
     */
    suspend fun flush()

    /**
     * Closes the sink. No further operation should be executed after the sink is closed.
     */
    override suspend fun close()

    /**
     * Writes a single byte to the sink
     */
    suspend fun writeByte(value: Byte): IKmpIoSink = write(byteArrayOf(value))

    /**
     * Writes a short to the sink
     */
    suspend fun writeShort(value: Short): IKmpIoSink = write(value.toBytes())

    /**
     * Writes a short in little-endian order to the sink
     */
    suspend fun writeShortLe(value: Short): IKmpIoSink = write(value.toBytesLe())

    /**
     * Writes an int to the sink
     */
    suspend fun writeInt(value: Int): IKmpIoSink = write(value.toBytes())

    /**
     * Writes an int in little-endian order to the sink
     */
    suspend fun writeIntLe(value: Int): IKmpIoSink = write(value.toBytesLe())

    /**
     * Writes a float to the sink
     */
    suspend fun writeFloat(value: Float): IKmpIoSink = write(value.toBytes())

    /**
     * Writes a float in little-endian order to the sink
     */
    suspend fun writeFloatLe(value: Float): IKmpIoSink = write(value.toBytesLe())

    /**
     * Writes a double to the sink
     */
    suspend fun writeDouble(value: Double): IKmpIoSink = write(value.toBytes())

    /**
     * Writes a double in little-endian order to the sink
     */
    suspend fun writeDoubleLe(value: Double): IKmpIoSink = write(value.toBytesLe())

    /**
     * Writes a long to the sink
     */
    suspend fun writeLong(value: Long): IKmpIoSink = write(value.toBytes())

    /**
     * Writes a long in little-endian order to the sink
     */
    suspend fun writeLongLe(value: Long): IKmpIoSink = write(value.toBytesLe())

    /**
     * Writes a UTF-8 encoded string to the sink
     */
    suspend fun writeUtf8(value: String): IKmpIoSink = write(value.encodeToByteArray())

    /**
     * Writes all bytes from the [source] to the sink
     */
    suspend fun writeAll(source: IKmpIoSource, bufferSize: Int = 16384): Long = source.readAll(this)
}
