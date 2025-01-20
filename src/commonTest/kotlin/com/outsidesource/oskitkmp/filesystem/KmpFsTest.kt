package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.annotation.ExperimentalOsKitApi
import com.outsidesource.oskitkmp.io.toKmpFsSink
import com.outsidesource.oskitkmp.io.toKmpFsSource
import com.outsidesource.oskitkmp.lib.encodeToHex
import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KmpFsTest {
    @OptIn(ExperimentalOsKitApi::class)
    @Test
    fun testByteArraySource() = runBlockingTest {
        val bytes = "this is the first line\nthis is the second line".encodeToByteArray().toKmpFsSource()
        assertEquals(bytes.readUtf8Line(), "this is the first line")
        assertEquals(bytes.readUtf8Line(), "this is the second line")
        assertFails { bytes.readByte() }
    }

    @OptIn(ExperimentalOsKitApi::class)
    @Test
    fun testByteArraySink() = runBlockingTest {
        val bytes = ByteArray(32)
        val sink = bytes.toKmpFsSink()
        sink.writeUtf8("this is a test")
        sink.writeUtf8("this is a test")
        assertEquals(bytes.encodeToHex(), "7468697320697320612074657374746869732069732061207465737400000000")
        assertFails { sink.writeUtf8("this is a test") }
    }
}