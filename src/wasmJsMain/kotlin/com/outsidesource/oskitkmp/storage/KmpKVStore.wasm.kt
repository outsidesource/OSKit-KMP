package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome

enum class WasmKmpKVStoreType {
    LocalStorage,
    IndexedDb,
}

class WasmKmpKVStore(
    private val type: WasmKmpKVStoreType = WasmKmpKVStoreType.LocalStorage,
) : IKmpKVStore {
    override fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> = try {
        val node = when (type) {
            WasmKmpKVStoreType.LocalStorage -> LocalStorageWasmKmpKVStoreNode(nodeName)
            WasmKmpKVStoreType.IndexedDb -> IndexedDbWasmKmpKVStoreNode(nodeName)
        }
        Outcome.Ok(node)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
