package com.outsidesource.oskitkmp.storage

class IosKmpKVStoreTest : KmpKVStoreTestBase() {
    override val kvStore: IKmpKVStore = IosKmpKVStore()
}