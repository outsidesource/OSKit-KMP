package com.outsidesource.oskitkmp.storage

actual fun createKmpKVStore(): IKmpKVStore {
    return WasmKmpKVStore(WasmKmpKVStoreType.LocalStorage)
}
