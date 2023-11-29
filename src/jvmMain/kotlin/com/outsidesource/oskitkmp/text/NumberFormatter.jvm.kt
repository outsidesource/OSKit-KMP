package com.outsidesource.oskitkmp.text

import java.text.DecimalFormat

actual class NumberFormatter actual constructor(
    private val minimumFractionDigits: Int,
    private val maximumFractionDigits: Int,
    private val useGrouping: Boolean,
) {
    private val formatter: DecimalFormat = DecimalFormat()

    init {
        formatter.isGroupingUsed = useGrouping
        formatter.minimumFractionDigits = minimumFractionDigits
        formatter.maximumFractionDigits = maximumFractionDigits
        formatter.isDecimalSeparatorAlwaysShown = false
    }

    actual fun format(value: Float): String = formatter.format(value)
    actual fun format(value: Double): String = formatter.format(value)
    actual fun format(value: Int): String = formatter.format(value)
    actual fun format(value: Long): String = formatter.format(value)

    actual companion object
}