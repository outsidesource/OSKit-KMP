package com.outsidesource.oskitkmp.lib

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.measureTime

class JsInteropTest {
    private val bytes = ByteArray(256) { it.toByte() }

    @Test
    fun byteArrayRoundTripsThroughArrayBuffer() {
        assertContentEquals(bytes, bytes.toArrayBuffer().toByteArray())
    }

    @Test
    fun toArrayBufferCopiesRequestedRange() {
        val buffer = bytes.toArrayBuffer(startIndex = 10, byteCount = 20)

        assertEquals(20, buffer.byteLength)
        assertContentEquals(bytes.copyOfRange(10, 30), buffer.toByteArray())
    }

    @Test
    fun copyIntoHonorsOffsetsAndRange() {
        val source = Uint8Array(bytes.toArrayBuffer())
        val destination = ByteArray(50)

        source.copyInto(destination, destinationOffset = 5, startIndex = 100, endIndex = 140)

        assertContentEquals(ByteArray(5), destination.copyOfRange(0, 5))
        assertContentEquals(bytes.copyOfRange(100, 140), destination.copyOfRange(5, 45))
        assertContentEquals(ByteArray(5), destination.copyOfRange(45, 50))
    }

    @Test
    fun emptyArraysConvert() {
        assertEquals(0, ByteArray(0).toArrayBuffer().byteLength)
        assertEquals(0, ArrayBuffer(0).toByteArray().size)
    }

    @Test
    fun largeArrayRoundTrips() {
        val large = ByteArray(5 * 1024 * 1024) { (it * 31).toByte() }
        lateinit var result: ByteArray

        val duration = measureTime { result = large.toArrayBuffer().toByteArray() }

        println("5 MB round trip took $duration")
        assertContentEquals(large, result)
    }
}
