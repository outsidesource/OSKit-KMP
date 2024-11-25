package com.outsidesource.oskitkmp.storage

import androidx.test.core.app.ApplicationProvider

class AndroidKmpKVStoreTest() : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = AndroidKmpKVStore(ApplicationProvider.getApplicationContext())
}
