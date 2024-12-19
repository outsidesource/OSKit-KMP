package com.outsidesource.oskitkmp.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitsTest {

    private val number = 0b101010
    private val longNumber = 0b10101010101010110101010101010010101010101

    @Test
    fun getBitTest() {
        assertTrue(Bits.getBit(number, 3))
        assertFalse(Bits.getBit(number.toShort(), 2))
        assertTrue(Bits.getBit(longNumber, 4))
    }

    @Test
    fun setBitTest() {
        assertEquals(0b101011, Bits.setBit(number, 0))
        assertEquals(0b111010, Bits.setBit(number.toShort(), 4))
        assertEquals(0b10101010101010110101010101010010101010111, Bits.setBit(longNumber, 1))
    }

    @Test
    fun unsetBitTest() {
        assertEquals(0b101000, Bits.unsetBit(number, 1))
        assertEquals(0b001010, Bits.unsetBit(number.toShort(), 5))
        assertEquals(0b10101010101010110101010101010010101010001, Bits.unsetBit(longNumber, 2))
    }

    @Test
    fun getBitsTest() {
        val number = 0b101010
        assertEquals(0b101, Bits.getBits(number, 1, 3))
        assertEquals(0b10, Bits.getBits(number.toShort(), 4, 5))
        assertEquals(0b0101010, Bits.getBits(longNumber, 1, 7))
    }

    @Test
    fun setBitsTest() {
        val number = 0b101010
        assertEquals(0b101110, Bits.setBits(number, 1, 3, 0b111))
        assertEquals(0b001010, Bits.setBits(number.toShort(), 4, 5, 0b00))
        assertEquals(0b10101010101010110101010101010010101011101, Bits.setBits(longNumber, 2, 4, 0b111))
    }

    @Test
    fun unsetBitsTest() {
        val number = 0b101010
        assertEquals(0b100000, Bits.unsetBits(number, 1, 3))
        assertEquals(0b001010, Bits.unsetBits(number.toShort(), 4, 5))
        assertEquals(0b10101010101010110101010101010010101000101, Bits.unsetBits(longNumber, 3, 5))
    }

    @Test
    fun flipBitTest() {
        val number = 0b101010
        assertEquals(0b101011, Bits.flipBit(number, 0))
        assertEquals(0b111010, Bits.flipBit(number.toShort(), 4))
        assertEquals(0b10101010101010110101010101010010101010001, Bits.flipBit(longNumber, 2))
    }

    @Test
    fun flipBitsTest() {
        val number = 0b101010
        assertEquals(0b100100, Bits.flipBits(number, 1, 3))
        assertEquals(0b011010, Bits.flipBits(number.toShort(), 4, 5))
        assertEquals(0b10101010101010110101010101010010101001001, Bits.flipBits(longNumber, 2, 4))
    }
}