package com.outsidesource.oskitkmp.storage

import androidx.test.core.app.ApplicationProvider

actual fun createKmpKVStore(): IKmpKVStore {
    return AndroidKmpKVStore(ApplicationProvider.getApplicationContext())
}
