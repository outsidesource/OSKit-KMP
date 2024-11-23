package com.outsidesource.oskitkmp.storage

actual fun createKMPStorage(): IKMPStorage {
    return WasmKmpStorage(WasmKmpStorageType.LocalStorage)
}
