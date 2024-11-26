package com.outsidesource.oskitkmp.storage

class LocalStorageWasmKmpKVStoreTest : KmpKVStoreTestBase() {
    override val kvStore: IKmpKVStore = WasmKmpKVStore(WasmKmpKVStoreType.LocalStorage)
}

class IndexedDbWasmKmpKVStoreTest : KmpKVStoreTestBase() {
    override val kvStore: IKmpKVStore = WasmKmpKVStore(WasmKmpKVStoreType.IndexedDb)
}
