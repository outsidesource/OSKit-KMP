package com.outsidesource.oskitkmp.text

actual class NumberFormatter actual constructor(
    private val minimumFractionDigits: Int,
    private val maximumFractionDigits: Int,
    private val useGrouping: Boolean,
) {

    actual fun format(value: Float): String = ""
    actual fun format(value: Double): String = ""
    actual fun format(value: Int): String = ""
    actual fun format(value: Long): String = ""

    actual companion object
}
