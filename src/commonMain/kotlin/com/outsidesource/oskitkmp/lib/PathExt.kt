package com.outsidesource.oskitkmp.lib

import okio.Path

fun Path.resolveSibling(name: String): Path {
    val localParent = parent ?: return resolve(name)
    return localParent.resolve(name)
}

val Path.pathString: String
    get() = toString()
