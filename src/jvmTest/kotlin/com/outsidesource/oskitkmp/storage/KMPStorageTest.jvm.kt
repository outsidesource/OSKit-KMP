package com.outsidesource.oskitkmp.storage

class JvmKmpKVStoreTest : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = JvmKmpKVStore("oskit-kmp")
}