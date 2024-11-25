package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

enum class WasmKmpKVStoreType {
    LocalStorage,
    IndexedDb,
}

class WasmKmpKVStore(
    private val type: WasmKmpKVStoreType = WasmKmpKVStoreType.LocalStorage,
) : IKmpKVStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> = try {
        val node = when (type) {
            WasmKmpKVStoreType.LocalStorage -> LocalStorageWasmKmpKVStoreNode(nodeName)
            WasmKmpKVStoreType.IndexedDb -> IndexedDbWasmKmpKVStoreNode(nodeName).apply {
                open().unwrapOrReturn { return Outcome.Error(Exception("Could not open node")) }
            }
        }
        Outcome.Ok(node)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
