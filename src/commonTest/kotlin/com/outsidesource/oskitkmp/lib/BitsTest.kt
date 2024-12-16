package com.outsidesource.oskitkmp.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitsTest {

    @Test
    fun getBitTest() {
        val number = 0b101010
        assertTrue(getBit(number, 3))
        assertFalse(getBit(number, 2))
        assertTrue(getBit(number, 5))
    }

    @Test
    fun setBitTest() {
        val number = 0b101010
        assertEquals(0b101011, setBit(number, 0))
        assertEquals(0b111010, setBit(number, 4))
    }

    @Test
    fun unsetBitTest() {
        val number = 0b101010
        assertEquals(0b101000, unsetBit(number, 1))
        assertEquals(0b001010, unsetBit(number, 5))
    }

    @Test
    fun getBitsTest() {
        val number = 0b101010
        assertEquals(0b101, getBits(number, 1, 3))
        assertEquals(0b10, getBits(number, 4, 5))
    }

    @Test
    fun setBitsTest() {
        val number = 0b101010
        assertEquals(0b101110, setBits(number, 1, 3, 0b111))
        assertEquals(0b001010, setBits(number, 4, 5, 0b00))
    }

    @Test
    fun unsetBitsTest() {
        val number = 0b101010
        assertEquals(0b100000, unsetBits(number, 1, 3))
        assertEquals(0b001010, unsetBits(number, 4, 5))
    }

    @Test
    fun flipBitTest() {
        val number = 0b101010
        assertEquals(0b101011, flipBit(number, 0))
        assertEquals(0b111010, flipBit(number, 4))
    }

    @Test
    fun flipBitsTest() {
        val number = 0b101010
        assertEquals(0b100100, flipBits(number, 1, 3))
        assertEquals(0b011010, flipBits(number, 4, 5))
    }
}