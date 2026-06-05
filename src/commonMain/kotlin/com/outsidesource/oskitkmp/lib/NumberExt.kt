package com.outsidesource.oskitkmp.lib

import kotlin.math.round

fun Int.snapTo(value: Int): Int {
    require(value > 0)

    val current = this.toLong()
    val step = value.toLong()
    val halfStep = step / 2

    val snapped = if (current >= 0) {
        (current + halfStep) / step * step
    } else {
        (current - halfStep) / step * step
    }

    return snapped.toInt()
}

fun Float.snapTo(value: Float): Float {
    require(value != 0f)
    return round(this / value) * value
}

operator fun ClosedRange<Float>.times(value: Float) = (start * value)..(endInclusive * value)
operator fun ClosedRange<Float>.div(value: Float) = (start / value)..(endInclusive / value)
