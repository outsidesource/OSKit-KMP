package com.outsidesource.oskitkmp.storage

class LocalStorageWasmKmpKVStoreTest : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = WasmKmpKVStore(WasmKmpKVStoreType.LocalStorage)
}

class IndexedDbLWasmKmpKVStoreTest : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = WasmKmpKVStore(WasmKmpKVStoreType.IndexedDb)
}
