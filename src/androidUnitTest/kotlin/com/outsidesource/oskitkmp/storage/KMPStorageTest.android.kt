package com.outsidesource.oskitkmp.storage

import androidx.test.core.app.ApplicationProvider

actual fun createKMPStorage(): IKMPStorage {
    return AndroidKMPStorage(ApplicationProvider.getApplicationContext())
}
