package com.outsidesource.oskitkmp.storage

actual fun createKmpKVStore(): IKmpKVStore {
    return DesktopKmpKVStore("oskit-kmp")
}