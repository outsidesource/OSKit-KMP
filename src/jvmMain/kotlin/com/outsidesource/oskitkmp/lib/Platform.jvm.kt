package com.outsidesource.oskitkmp.lib

actual val Platform.Companion.current: Platform by lazy {
    val os = System.getProperty("os.name")
    when {
        os.equals("Mac OS X", ignoreCase = true) -> Platform.MacOS
        os.startsWith("Win", ignoreCase = true) -> Platform.Windows
        os.startsWith("Linux", ignoreCase = true) -> Platform.Linux
        else -> Platform.Unknown
    }
}
