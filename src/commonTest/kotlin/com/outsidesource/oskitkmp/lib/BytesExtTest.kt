package com.outsidesource.oskitkmp.lib

import kotlin.test.Test
import kotlin.test.assertTrue

class BytesExtTest {
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    @Test
    fun testFind() {
        val data = "0000000000000000000001020304050600000000".hexToByteArray()
        assertTrue(data.find("010203040506".hexToByteArray()) == 10, "Incorrect position")
        assertTrue(data.find("01020304050607".hexToByteArray()) == -1, "Should not have found")
    }

    @Test
    fun testShortConversion() {
        val bytes1 = byteArrayOf(0x00, 0x01)
        val bytes2 = byteArrayOf(0x01, 0x00)
        val value1 = 1.toShort()
        val value2 = 256.toShort()
        assertTrue("Conversion value 1") { bytes1.toShort() == value1 }
        assertTrue("Conversion value 2") { bytes2.toShort() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toShortLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toShortLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @Test
    fun testUShortConversion() {
        val bytes1 = byteArrayOf(0x00, 0x80.toByte())
        val bytes2 = byteArrayOf(0x80.toByte(), 0x00)
        val value1 = 128u.toUShort()
        val value2 = 32768u.toUShort()
        assertTrue("Conversion value 1") { bytes1.toUShort() == value1 }
        assertTrue("Conversion value 2") { bytes2.toUShort() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toUShortLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toUShortLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @Test
    fun testIntConversion() {
        val bytes1 = byteArrayOf(0x00, 0x00, 0x00, 0x01)
        val bytes2 = byteArrayOf(0x01, 0x00, 0x00, 0x00)
        val value1 = 1
        val value2 = 16777216
        assertTrue("Conversion value 1") { bytes1.toInt() == value1 }
        assertTrue("Conversion value 2") { bytes2.toInt() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toIntLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toIntLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @Test
    fun testUIntConversion() {
        val bytes1 = byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte())
        val bytes2 = byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00)
        val value1 = 128u
        val value2 = 2147483648u
        assertTrue("Conversion value 1") { bytes1.toUInt() == value1 }
        assertTrue("Conversion value 2") { bytes2.toUInt() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toUIntLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toUIntLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @Test
    fun testLongConversion() {
        val bytes1 = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
        val bytes2 = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val value1 = 1L
        val value2 = 72057594037927936L
        assertTrue("Conversion value 1") { bytes1.toLong() == value1 }
        assertTrue("Conversion value 2") { bytes2.toLong() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toLongLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toLongLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @Test
    fun testULongConversion() {
        val bytes1 = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80.toByte())
        val bytes2 = byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val value1 = 128u.toULong()
        val value2 = 9223372036854775808u
        assertTrue("Conversion value 1") { bytes1.toULong() == value1 }
        assertTrue("Conversion value 2") { bytes2.toULong() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toULongLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toULongLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testFloatConversion() {
        val bytes1 = byteArrayOf(0x00, 0x00, 0x00, 0x01)
        val bytes2 = byteArrayOf(0x01, 0x00, 0x00, 0x00)
        val value1 = 1e-45f
        val value2 = 2.3509887e-38f
        assertTrue("Conversion value 1") { bytes1.toFloat() == value1 }
        assertTrue("Conversion value 2") { bytes2.toFloat() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toFloatLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toFloatLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }

    @Test
    fun testDoubleConversion() {
        val bytes1 = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
        val bytes2 = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val value1 = 4.9E-324
        val value2 = 7.2911220195563975E-304
        assertTrue("Conversion value 1") { bytes1.toDouble() == value1 }
        assertTrue("Conversion value 2") { bytes2.toDouble() == value2 }
        assertTrue("Conversion value le 1") { bytes2.toDoubleLe() == value1 }
        assertTrue("Conversion value le 2") { bytes1.toDoubleLe() == value2 }
        assertTrue("Conversion to bytes 1") { value1.toBytes().contentEquals(bytes1) }
        assertTrue("Conversion to bytes 2") { value2.toBytes().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 1") { value1.toBytesLe().contentEquals(bytes2) }
        assertTrue("Conversion to bytes le 2") { value2.toBytesLe().contentEquals(bytes1) }
    }
}