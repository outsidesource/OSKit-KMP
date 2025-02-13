package com.outsidesource.oskitkmp.lib

enum class Platform {
    Android,
    IOS,
    MacOS,
    Windows,
    Linux,
    WebBrowser,
    Unknown,
    ;

    val isMobile
        get() = when (this) {
            IOS, Android -> true
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
