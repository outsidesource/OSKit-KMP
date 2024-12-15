package com.outsidesource.oskitkmp.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class BytesExtTest {
    @Test
    fun testIntToBytesLe() {
        val value = 12345678
        val bytes = value.toBytesLe()
        val result = bytes.toIntLe()
        assertEquals(value, result)
    }

    @Test
    fun testShortToBytesLe() {
        val value: Short = 32000
        val bytes = value.toBytesLe()
        val result = bytes.toShortLe()
        assertEquals(value, result)
    }

    @Test
    fun testLongToBytesLe() {
        val value = 1234567890123L
        val bytes = value.toBytesLe()
        val result = bytes.toLongLe()
        assertEquals(value, result)
    }

    @Test
    fun testFloatToBytesLe() {
        val value = 123456.78f
        val bytes = value.toBytesLe()
        val result = bytes.toFloatLe()
        assertEquals(value, result)
    }

    @Test
    fun testDoubleToBytesLe() {
        val value = 12345678.90
        val bytes = value.toBytesLe()
        val result = bytes.toDoubleLe()
        assertEquals(value, result)
    }
}