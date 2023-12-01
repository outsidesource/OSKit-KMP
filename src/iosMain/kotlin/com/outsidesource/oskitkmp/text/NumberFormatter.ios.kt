package com.outsidesource.oskitkmp.text

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterNoStyle

actual class NumberFormatter actual constructor(
    private val minimumFractionDigits: Int,
    private val maximumFractionDigits: Int,
    private val useGrouping: Boolean,
) {
    private val formatter = NSNumberFormatter()

    init {
        formatter.minimumFractionDigits = minimumFractionDigits.toULong()
        formatter.maximumFractionDigits = maximumFractionDigits.toULong()

        if (useGrouping) {
            formatter.numberStyle = NSNumberFormatterDecimalStyle
        } else {
            formatter.numberStyle = NSNumberFormatterNoStyle
        }
    }

    actual fun format(value: Float): String = formatter.stringFromNumber(NSNumber(float = value)) ?: ""
    actual fun format(value: Double): String = formatter.stringFromNumber(NSNumber(double = value)) ?: ""
    actual fun format(value: Int): String = formatter.stringFromNumber(NSNumber(int = value)) ?: ""
    actual fun format(value: Long): String = formatter.stringFromNumber(NSNumber(long = value)) ?: ""

    actual companion object
}
