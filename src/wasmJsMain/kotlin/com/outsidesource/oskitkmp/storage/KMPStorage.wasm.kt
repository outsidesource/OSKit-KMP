package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class WasmKmpStorageType {
    LocalStorage,
    IndexedDb,
}

class WasmKmpStorage(
    private val type: WasmKmpStorageType = WasmKmpStorageType.LocalStorage,
) : IKMPStorage {
    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> = try {
        val node = when (type) {
            WasmKmpStorageType.LocalStorage -> LocalStorageWasmKmpStorageNode(nodeName)
            WasmKmpStorageType.IndexedDb -> IndexedDbWasmKmpStorageNode(nodeName)
        }
        Outcome.Ok(node)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal object WasmQueryRegistry {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val listeners: AtomicRef<Map<String, List<() -> Unit>>> = atomic(emptyMap())

    fun addListener(key: String, listener: () -> Unit) = listeners.update {
        it.toMutableMap().apply {
            val listeners = (this[key] ?: emptyList()) + listener
            this[key] = listeners
        }
    }

    fun removeListener(key: String, listener: () -> Unit) = listeners.update {
        it.toMutableMap().apply {
            val listeners = (this[key] ?: emptyList()) - listener
            this[key] = listeners
        }
    }

    fun notifyListeners(key: String) {
        scope.launch { listeners.value[key]?.forEach { it() } }
    }
}
