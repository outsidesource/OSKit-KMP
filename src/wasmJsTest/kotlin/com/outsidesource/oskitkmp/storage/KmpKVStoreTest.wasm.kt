package com.outsidesource.oskitkmp.storage

class LocalStorageWasmKmpKVStoreTest : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = WasmKmpKVStore(WasmKmpKVStoreType.LocalStorage)
}

class IndexedDbWasmKmpKVStoreTest : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = WasmKmpKVStore(WasmKmpKVStoreType.IndexedDb)
}
