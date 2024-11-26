package com.outsidesource.oskitkmp.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class AndroidKmpKVStoreTest() : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = AndroidKmpKVStore(ApplicationProvider.getApplicationContext())
}
