package com.outsidesource.oskitkmp.lib

fun <T : Any?> T.printed(): T = apply { println(this) }

fun printAll(vararg args: Any?) = println(args.joinToString(", "))
