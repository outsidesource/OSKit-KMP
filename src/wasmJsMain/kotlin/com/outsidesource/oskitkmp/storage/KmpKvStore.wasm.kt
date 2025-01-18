package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

enum class WasmKmpKvStoreType {
    LocalStorage,
    IndexedDb,
}

fun KmpKvStore(type: WasmKmpKvStoreType = WasmKmpKvStoreType.IndexedDb): IKmpKvStore = WasmKmpKvStore(type)

internal class WasmKmpKvStore(
    private val type: WasmKmpKvStoreType = WasmKmpKvStoreType.IndexedDb,
) : IKmpKvStore {
    override suspend fun openNode(nodeName: String): Outcome<IKmpKvStoreNode, Exception> = try {
        val node = when (type) {
            WasmKmpKvStoreType.LocalStorage -> LocalStorageWasmKmpKvStoreNode(nodeName)
            WasmKmpKvStoreType.IndexedDb -> IndexedDbWasmKmpKvStoreNode(nodeName).apply {
                open().unwrapOrReturn { return Outcome.Error(Exception("Could not open node")) }
            }
        }
        Outcome.Ok(node)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
