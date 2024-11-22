package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class WASMKMPStorageType {
    LocalStorage,
    IndexedDB,
}

class WASMKMPStorage(
    private val type: WASMKMPStorageType = WASMKMPStorageType.LocalStorage,
) : IKMPStorage {
    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> = try {
        val node = when (type) {
            WASMKMPStorageType.LocalStorage -> WasmLocalStorageKmpStorageNode(nodeName)
            WASMKMPStorageType.IndexedDB -> WASMIndexedDBKMPStorageNode(nodeName)
        }
        Outcome.Ok(node)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

internal object WASMQueryRegistry {
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
