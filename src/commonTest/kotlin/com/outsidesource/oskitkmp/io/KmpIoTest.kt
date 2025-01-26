package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.lib.encodeToHex
import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KmpIoTest {
    @Test
    fun testByteArraySource() = runBlockingTest {
        val source = "this is the first line\nthis is the second line".encodeToByteArray().toKmpIoSource()
        assertEquals(source.readUtf8Line(), "this is the first line")
        assertEquals(source.readUtf8Line(), "this is the second line")
        assertFails { source.readByte() }
    }

    @Test
    fun testByteArraySink() = runBlockingTest {
        val bytes = ByteArray(32)
        val sink = bytes.toKmpIoSink()
        sink.writeUtf8("this is a test")
        sink.writeUtf8("this is a test")
        assertEquals(bytes.encodeToHex(), "7468697320697320612074657374746869732069732061207465737400000000")
        assertFails { sink.writeUtf8("this is a test") }
    }

    @Test
    fun testReadAll() = runBlockingTest {
        val source = "this is a test source".encodeToByteArray().toKmpIoSource()
        val sinkBytes = ByteArray(21)
        val sink = sinkBytes.toKmpIoSink()
        sink.writeAll(source, bufferSize = 16)
        assertEquals(sinkBytes.decodeToString(), "this is a test source")
    }
}