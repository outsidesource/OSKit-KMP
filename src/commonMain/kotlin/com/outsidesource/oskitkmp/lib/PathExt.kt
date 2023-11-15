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

fun Path.resolveSibling(name: String): Path {
    val localParent = parent ?: return resolve(name)
    return localParent.resolve(name)
}

val Path.pathString: String
    get() = toString()
