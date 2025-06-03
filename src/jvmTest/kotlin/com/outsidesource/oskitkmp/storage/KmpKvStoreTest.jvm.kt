package com.outsidesource.oskitkmp.storage

internal actual fun getPlatformKmpKvStores(): List<IKmpKvStore> = listOf(
    JvmKmpKvStore("oskit-kmp"),
    InMemoryKmpKvStore(),
)