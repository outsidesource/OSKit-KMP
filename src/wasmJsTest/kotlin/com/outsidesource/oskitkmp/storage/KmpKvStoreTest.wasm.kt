package com.outsidesource.oskitkmp.storage

class LocalStorageWasmKmpKvStoreTest : KmpKvStoreTestBase() {
    override val kvStore: IKmpKvStore = WasmKmpKvStore(WasmKmpKvStoreType.LocalStorage)
}

class IndexedDbWasmKmpKvStoreTest : KmpKvStoreTestBase() {
    override val kvStore: IKmpKvStore = WasmKmpKvStore(WasmKmpKvStoreType.IndexedDb)
}
