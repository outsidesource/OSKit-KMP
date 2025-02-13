package com.outsidesource.oskitkmp.storage

class IosKmpKvStoreTest : KmpKvStoreTestBase() {
    override val kvStore: IKmpKvStore = IosKmpKvStore()
}