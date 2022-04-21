package com.outsidesource.oskitkmp.lib

enum class Platform {
    Android,
    iOS,
    MacOS,
    Windows,
    Linux,
    Unknown;

    companion object
}

expect val Platform.Companion.current: Platform
