package com.outsidesource.oskitkmp.lib

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun <T : Any?> T.printed(): T = apply { println(this) }

fun printAll(vararg args: Any?) = println(args.joinToString(", "))

@OptIn(ExperimentalTime::class)
suspend inline fun <T> measureTimePrinted(tag: String? = null, block: suspend () -> T): T {
    val start = Clock.System.now()
    val result = block()
    println(
        "Time Elapsed${if (tag != null) " - $tag - " else " - "}${(Clock.System.now() - start).inWholeMilliseconds}ms",
    )
    return result
}
