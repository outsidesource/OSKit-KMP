package com.outsidesource.oskitkmp.lib

import kotlin.math.absoluteValue

fun Int.snapTo(value: Int): Int {
    require(value > 0f)

    if (value == 1) return this

    val diff = (this % value).absoluteValue
    return if (this > 0) {
        if (diff >= (value / 2)) this + (value - diff) else this - diff
    } else {
        if (diff >= (value / 2)) this - (value - diff) else this + diff
    }
}

fun Float.snapTo(value: Float): Float {
    require(value > 0f)
    val diff = (this % value).absoluteValue
    return if (this > 0) {
        if (diff >= (value / 2)) this + (value - diff) else this - diff
    } else {
        if (diff >= (value / 2)) this - (value - diff) else this + diff
    }
}

operator fun ClosedRange<Float>.times(value: Float) = (start * value)..(endInclusive * value)
operator fun ClosedRange<Float>.div(value: Float) = (start / value)..(endInclusive / value)
