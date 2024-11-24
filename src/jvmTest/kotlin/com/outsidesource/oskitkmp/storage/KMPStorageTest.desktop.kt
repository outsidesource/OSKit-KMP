package com.outsidesource.oskitkmp.storage

actual fun createKmpKVStore(): IKmpKVStore {
    return JvmKmpKVStore("oskit-kmp")
}