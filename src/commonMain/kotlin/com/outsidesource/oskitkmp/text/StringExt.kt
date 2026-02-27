package com.outsidesource.oskitkmp.text

private val numberRegex = Regex("[^\\.0-9\\-]")

// Fixes Kotlin toFloatOrNull not handling ',' characters
fun String.parseFloatOrNull(): Float? = replace(numberRegex, "").toFloatOrNull()
