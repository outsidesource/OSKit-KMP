package com.outsidesource.oskitkmp.lib

enum class Platform {
    Android,
    iOS,
    MacOS,
    Windows,
    Linux,
    Unknown;

    val isMobile
        get() = when (this) {
            iOS, Android -> true
            else -> false
        }

    val isDesktop
        get() = when (this) {
            MacOS, Windows, Linux -> true
            else -> false
        }

    companion object
}

expect val Platform.Companion.current: Platform
