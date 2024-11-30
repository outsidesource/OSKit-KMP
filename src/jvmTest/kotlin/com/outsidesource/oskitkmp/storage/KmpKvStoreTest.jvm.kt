package com.outsidesource.oskitkmp.storage

class JvmKmpKvStoreTest : KmpKvStoreTestBase() {
    override val kvStore: IKmpKvStore = JvmKmpKvStore("oskit-kmp")
}