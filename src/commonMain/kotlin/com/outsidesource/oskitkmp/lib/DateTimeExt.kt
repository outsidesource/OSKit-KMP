package com.outsidesource.oskitkmp.lib

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

fun LocalDateTime.kmpFormatDateTime(format: String): String {
    val hourAmPmMode = this.time.convertTo12HourFormat()
    val stringBuilder = StringBuilder()

    var i = 0
    while (i < format.length) {
        when (val currentChar = format[i]) {
            'a' -> stringBuilder.append(hourAmPmMode.second.name)
            '@' -> stringBuilder.append("at")
            'd' -> {
                if (i + 1 < format.length && format[i + 1] == 'd') {
                    stringBuilder.append(
                        if (this.dayOfMonth < 10) "0${this.dayOfMonth}" else this.dayOfMonth.toString(),
                    )
                    i++
                } else {
                    stringBuilder.append(this.dayOfMonth.toString())
                }
            }
            'M' -> {
                if (i + 3 < format.length && format.substring(i, i + 4) == "MMMM") {
                    stringBuilder.append(this.month.getDisplayName(DateTextFormat.Full))
                    i += 3
                } else if (i + 2 < format.length && format.substring(i, i + 3) == "MMM") {
                    stringBuilder.append(this.month.getDisplayName(DateTextFormat.Short))
                    i += 2
                } else if (i + 1 < format.length && format[i + 1] == 'M') {
                    stringBuilder.append(
                        if (this.monthNumber < 10) "0${this.monthNumber}" else this.monthNumber.toString(),
                    )
                    i++
                } else {
                    stringBuilder.append(this.monthNumber.toString())
                }
            }
            'y' -> {
                if (i + 3 < format.length && format.substring(i, i + 4) == "yyyy") {
                    stringBuilder.append(this.year.toString())
                    i += 3
                } else if (i + 1 < format.length && format[i + 1] == 'y') {
                    stringBuilder.append((this.year % 100).toString().padStart(2, '0'))
                    i++
                }
            }
            'h' -> {
                if (i + 1 < format.length && format[i + 1] == 'h') {
                    stringBuilder.append(hourAmPmMode.first.toString().padStart(2, '0'))
                    i++
                } else {
                    stringBuilder.append(hourAmPmMode.first.toString())
                }
            }
            'm' -> {
                if (i + 1 < format.length && format[i + 1] == 'm') {
                    stringBuilder.append(this.minute.toString().padStart(2, '0'))
                    i++
                } else {
                    stringBuilder.append(this.minute.toString())
                }
            }
            's' -> {
                if (i + 1 < format.length && format[i + 1] == 's') {
                    stringBuilder.append(this.second.toString().padStart(2, '0'))
                    i++
                } else {
                    stringBuilder.append(this.second.toString())
                }
            }
            'w' -> stringBuilder.append(this.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() })
            else -> stringBuilder.append(currentChar)
        }
        i++
    }

    return stringBuilder.toString()
}

private fun LocalTime.convertTo12HourFormat(): Pair<Int, Meridiem> {
    val hour = this.hour
    val period = if (hour < 12) Meridiem.AM else Meridiem.PM
    var hour12 = hour % 12

    if (hour12 == 0) {
        hour12 = 12
    }

    return Pair(hour12, period)
}

private fun Month.getDisplayName(format: DateTextFormat): String = when (this) {
    Month.JANUARY -> when (format) {
        DateTextFormat.Full -> "January"
        DateTextFormat.Short -> "Jan"
    }
    Month.FEBRUARY -> when (format) {
        DateTextFormat.Full -> "February"
        DateTextFormat.Short -> "Feb"
    }
    Month.MARCH -> when (format) {
        DateTextFormat.Full -> "March"
        DateTextFormat.Short -> "Mar"
    }
    Month.APRIL -> when (format) {
        DateTextFormat.Full -> "April"
        DateTextFormat.Short -> "Apr"
    }
    Month.MAY -> when (format) {
        DateTextFormat.Full -> "May"
        DateTextFormat.Short -> "May"
    }
    Month.JUNE -> when (format) {
        DateTextFormat.Full -> "June"
        DateTextFormat.Short -> "Jun"
    }
    Month.JULY -> when (format) {
        DateTextFormat.Full -> "July"
        DateTextFormat.Short -> "Jul"
    }
    Month.AUGUST -> when (format) {
        DateTextFormat.Full -> "August"
        DateTextFormat.Short -> "Aug"
    }
    Month.SEPTEMBER -> when (format) {
        DateTextFormat.Full -> "September"
        DateTextFormat.Short -> "Sep"
    }
    Month.OCTOBER -> when (format) {
        DateTextFormat.Full -> "October"
        DateTextFormat.Short -> "Oct"
    }
    Month.NOVEMBER -> when (format) {
        DateTextFormat.Full -> "November"
        DateTextFormat.Short -> "Nov"
    }
    Month.DECEMBER -> when (format) {
        DateTextFormat.Full -> "December"
        DateTextFormat.Short -> "Dec"
    }
    else -> ""
}

private enum class Meridiem {
    AM, PM
}

private enum class DateTextFormat {
    Short,
    Full,
}
