package com.outsidesource.oskitkmp.storage

class JvmKmpKVStoreTest : KmpKVStoreTestBase() {
    override val kvStore: IKmpKVStore = JvmKmpKVStore("oskit-kmp")
}