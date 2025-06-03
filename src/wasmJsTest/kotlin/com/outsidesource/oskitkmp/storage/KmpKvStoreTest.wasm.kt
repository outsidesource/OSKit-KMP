package com.outsidesource.oskitkmp.storage

internal actual fun getPlatformKmpKvStores(): List<IKmpKvStore> = listOf(
    WasmKmpKvStore(WasmKmpKvStoreType.LocalStorage),
    WasmKmpKvStore(WasmKmpKvStoreType.IndexedDb),
)
