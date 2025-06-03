package com.outsidesource.oskitkmp.storage

import androidx.test.core.app.ApplicationProvider

internal actual fun getPlatformKmpKvStores(): List<IKmpKvStore> = listOf(
    AndroidKmpKvStore(ApplicationProvider.getApplicationContext()),
)
