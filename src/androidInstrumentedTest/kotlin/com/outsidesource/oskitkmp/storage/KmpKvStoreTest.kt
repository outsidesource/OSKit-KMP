package com.outsidesource.oskitkmp.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class AndroidKmpKvStoreTest() : KmpKvStoreTestBase() {
    override val kvStore: IKmpKvStore = AndroidKmpKvStore(ApplicationProvider.getApplicationContext())
}
