package com.outsidesource.oskitkmp.text

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.toFixed(decimalPlaces: Int): String {
    return (this * 10f.pow(decimalPlaces)).roundToInt().toString().let {
        it.replaceRange(it.length - decimalPlaces, it.length - decimalPlaces, ".")
    }
}

expect class NumberFormatter(
    minimumFractionDigits: Int = 0,
    maximumFractionDigits: Int = 2,
    useGrouping: Boolean = true,
) {
    fun format(value: Float): String
    fun format(value: Double): String
    fun format(value: Int): String
    fun format(value: Long): String

    companion object
}

val WholeNumberFormatter = NumberFormatter(maximumFractionDigits = 0, minimumFractionDigits = 0)
val TenthsNumberFormatter = NumberFormatter(maximumFractionDigits = 1, minimumFractionDigits = 1)
val HundredthsNumberFormatter = NumberFormatter
