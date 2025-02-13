package com.outsidesource.oskitkmp.lib

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal expect val fileSystem: FileSystem

fun Path.absolute(): Path =
    when {
        isAbsolute -> this
        else -> {
            val currentDir = "".toPath()
            fileSystem.canonicalize(currentDir) / (this)
        }
    }
