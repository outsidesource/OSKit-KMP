package com.outsidesource.oskitkmp.lib

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.number

fun LocalDateTime.kmpFormat(format: String): String {
    val twelveHour = time.convertTo12HourFormat()

    var lastCharacter: Char? = null
    var characterCount = 0

    return buildString {
        for (i in 0..format.length) { // Iterate passed the last character to flush the last pattern
            val currentChar = format.getOrNull(i)
            if (currentChar == lastCharacter || lastCharacter == null) {
                characterCount++
                lastCharacter = currentChar
                continue
            }

            when (lastCharacter) {
                'a' -> append(twelveHour.second.name)
                '@' -> append("at")
                'd' -> when (characterCount) {
                    2 -> append(if (day < 10) "0$day" else day.toString())
                    1 -> append(day)
                }
                'D' -> when (characterCount) {
                    1 -> append(dayOfYear)
                }
                'M' -> when (characterCount) {
                    4 -> append(month.getDisplayName(DateTextFormat.Full))
                    3 -> append(month.getDisplayName(DateTextFormat.Short))
                    2 -> append(if (month.number < 10) "0${month.number}" else month.number.toString())
                    1 -> append(month.number)
                }
                'y' -> when (characterCount) {
                    4 -> append(year)
                    2 -> append((year % 100).toString().padStart(2, '0'))
                }
                'H' -> when (characterCount) {
                    2 -> append(hour.toString().padStart(2, '0'))
                    1 -> append(hour)
                }
                'h' -> when (characterCount) {
                    2 -> append(twelveHour.first.toString().padStart(2, '0'))
                    1 -> append(twelveHour.first)
                }
                'm' -> when (characterCount) {
                    2 -> append(minute.toString().padStart(2, '0'))
                    1 -> append(minute)
                }
                's' -> when (characterCount) {
                    2 -> append(second.toString().padStart(2, '0'))
                    1 -> append(second)
                }
                'E' -> when (characterCount) {
                    4 -> append(dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() })
                    3 -> append(dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() })
                    1 -> append(dayOfWeek.name.take(1))
                }
                else -> append(lastCharacter)
            }

            characterCount = 1
            lastCharacter = currentChar
        }
    }
}

private fun LocalTime.convertTo12HourFormat(): Pair<Int, Meridiem> {
    val hour = this.hour
    val period = if (hour < 12) Meridiem.AM else Meridiem.PM
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
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
}

private enum class Meridiem {
    AM, PM
}

private enum class DateTextFormat {
    Short,
    Full,
}
