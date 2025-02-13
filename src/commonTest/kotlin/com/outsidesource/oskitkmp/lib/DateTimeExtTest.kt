package com.outsidesource.oskitkmp.lib

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeExtTest {

    @Test
    fun testFullDateFormat() {
        val dateTime = LocalDateTime(2024, 12, 15, 14, 5, 30)
        val format = "dd MMMM yyyy, hh:mm:ss a"
        val expected = "15 December 2024, 02:05:30 PM"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testShortDateFormat() {
        val dateTime = LocalDateTime(2024, 1, 3, 9, 7, 5)
        val format = "d MMM yy, h:m:s a"
        val expected = "3 Jan 24, 9:7:5 AM"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testDayOfWeek() {
        val dateTime = LocalDateTime(2024, 12, 15, 18, 45, 0)
        var format = "EEEE, MMM dd"
        var expected = "Sunday, Dec 15"
        assertEquals(expected, dateTime.kmpFormat(format))

        format = "EEE, MMM dd"
        expected = "Sun, Dec 15"
        assertEquals(expected, dateTime.kmpFormat(format))

        format = "E, MMM dd"
        expected = "S, Dec 15"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testEdgeCaseMonthLeadingZero() {
        val dateTime = LocalDateTime(2024, 3, 5, 0, 0, 0)
        val format = "MM/dd/yyyy"
        val expected = "03/05/2024"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testHour24Format() {
        val dateTime = LocalDateTime(2024, 12, 15, 23, 15, 0)
        val format = "HH:mm a"
        val expected = "23:15 PM"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testHour12Format() {
        val dateTime = LocalDateTime(2024, 12, 15, 0, 15, 0)
        val format = "hh:mm a"
        val expected = "12:15 AM"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testInvalidCharacters() {
        val dateTime = LocalDateTime(2024, 12, 15, 23, 59, 59)
        val format = "yyyy-MM-dd @ hh:mm:ss"
        val expected = "2024-12-15 at 11:59:59"
        assertEquals(expected, dateTime.kmpFormat(format))
    }

    @Test
    fun testDayOfYear() {
        val dateTime = LocalDateTime(2024, 12, 15, 23, 59, 59)
        val format = "D"
        val expected = "350"
        assertEquals(expected, dateTime.kmpFormat(format))
    }
}