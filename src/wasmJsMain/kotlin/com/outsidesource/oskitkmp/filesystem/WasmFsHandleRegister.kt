package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import com.outsidesource.oskitkmp.storage.IDBDatabase
import com.outsidesource.oskitkmp.storage.await
import com.outsidesource.oskitkmp.storage.indexedDB
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.Any
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Maintains in-memory and long-term persistence of a map of unique ids to FileSystemHandle. The unique ids are used
 * in the WASM implementation of [KmpFsRef].
 *
 * WASM only supports passing around FileSystemHandles which will not serialize properly in [KmpFsRef]. In addition,
 * WASM only supports persisting FileSystemHandles via IndexedDB. Calling [KmpFsRef.toPersistableBytes] or
 * [KmpFsRef.toPersistableString] will automatically create an entry in IndexedDb to allow for [KmpFs] to retrieve the
 * proper handle when the persisted [KmpFsRef] is loaded.
 */
@OptIn(ExperimentalTime::class)
internal object WasmFsHandleRegister {
    private val lock = SynchronizedObject()
    private val handles: MutableMap<String, Any> = mutableMapOf()
    private val counter = atomic(Random(Clock.System.now().toEpochMilliseconds()).nextLong())

    private const val DB_NAME = "oskit-kmp-fs"
    private const val OBJECT_STORE = "fs-handles"
    private val db = CompletableDeferred<IDBDatabase?>()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val localDb = indexedDB.open(DB_NAME, 1)
                .await { db, oldVersion, newVersion -> db.createObjectStore(OBJECT_STORE) }
                .unwrapOrNull()
            db.complete(localDb)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createUniqueKey(): String =
        Clock.System.now().epochSeconds.toHexString().takeLast(10) + counter.incrementAndGet().toHexString()

    fun putHandle(handle: Any): String {
        val key = createUniqueKey()
        synchronized(lock) { handles[key] = handle }
        return key
    }

    suspend fun getHandle(key: String, mode: WasmFsHandleAccessMode = WasmFsHandleAccessMode.Read): Any? {
        val inMemoryHandle = synchronized(lock) { handles[key] }
        if (inMemoryHandle != null) return inMemoryHandle

        val handle = db.await()
            ?.transaction(OBJECT_STORE)
            ?.objectStore(OBJECT_STORE)
            ?.get(key)
            ?.await()
            ?.unwrapOrReturn { return null }

        if (handle is FileSystemHandle) {
            val options = permissionOptions(mode = if (mode == WasmFsHandleAccessMode.Read) "read" else "readwrite")
            handle.requestPermission(options).kmpAwaitOutcome()
        }

        return handle
    }

    suspend fun persistHandle(key: String, handle: FileSystemHandle) {
        db.await()
            ?.transaction(OBJECT_STORE, "readwrite")
            ?.objectStore(OBJECT_STORE)
            ?.put(key = key, item = handle)
            ?.await()
    }

    suspend fun removePersistedHandle(key: String?) {
        db.await()
            ?.transaction(OBJECT_STORE, "readwrite")
            ?.objectStore(OBJECT_STORE)
            ?.run { if (key == null) clear() else delete(key) }
            ?.await()
    }
}

internal enum class WasmFsHandleAccessMode {
    Read,
    Write,
}
