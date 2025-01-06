package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.lib.jsTryOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.runOnError
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

external object indexedDB {
    fun open(name: String): IDBOpenDBRequest
    fun open(name: String, version: Int): IDBOpenDBRequest
}

external class IDBOpenDBRequest : JsAny {
    var onerror: (Event) -> Unit
    var onblocked: (Event) -> Unit
    var onsuccess: (IDBOpenSuccessEvent) -> Unit
    var onupgradeneeded: (IDBVersionChangeEvent) -> Unit
    val result: IDBDatabase
}

suspend fun IDBOpenDBRequest.await(
    onUpgradeNeeded: (IDBDatabase, oldVersion: Int, newVersion: Int) -> Unit,
): Outcome<IDBDatabase, Any> = suspendCoroutine { continuation ->
    onerror = { continuation.resume(Outcome.Error(it)) }
    onsuccess = { continuation.resume(Outcome.Ok(it.target.result)) }
    onblocked = { continuation.resume(Outcome.Error(it)) }
    onupgradeneeded = {
        jsTryOutcome {
            onUpgradeNeeded(it.target.result, it.oldVersion, it.newVersion)
            (0).toJsNumber()
        }.runOnError {
            continuation.resume(Outcome.Error(it))
        }
    }
}

external class IDBRequest<T : JsAny?> : JsAny {
    val error: JsAny
    val readyState: JsString
    val source: JsAny
    val transaction: JsAny
    var onerror: (Event) -> Unit
    var onsuccess: (Event) -> Unit
    val result: T
}

external interface IDBOpenSuccessEvent : JsAny {
    val target: IDBRequestEventTarget
}

external class Event : JsAny {
    val error: Error?
    val target: JsAny
}

external class Error : JsAny {
    val message: String
}

external class IDBVersionChangeEvent : JsAny {
    val oldVersion: Int
    val newVersion: Int
    val target: IDBRequestEventTarget
}

external interface IDBRequestEventTarget : JsAny {
    val result: IDBDatabase
}

external class IDBDatabase : JsAny {
    fun close()
    fun createObjectStore(name: String): IDBObjectStore
    fun createObjectStore(name: String, options: JsAny): IDBObjectStore
    fun deleteObjectStore(name: String)
    fun transaction(storeName: String): IDBTransaction
    fun transaction(storeName: String, mode: String): IDBTransaction
    fun transaction(storeNames: JsArray<JsString>): IDBTransaction
    fun transaction(storeNames: JsArray<JsString>, mode: String): IDBTransaction
    fun transaction(storeNames: JsArray<JsString>, mode: String, options: JsAny): IDBTransaction
}

fun createObjectStoreOptions(keyPath: String, autoIncrement: Boolean): JsAny = js(
    """({keyPath: keyPath, autoIncrement: autoIncrement})""",
)
fun createObjectStoreOptions(keyPath: String): JsAny = js("""({keyPath: keyPath})""")
fun createObjectStoreOptions(autoIncrement: Boolean): JsAny = js("""({autoIncrement: autoIncrement})""")

external class IDBObjectStore : JsAny {
    val keyPath: String?
    val autoIncrement: Boolean
    val name: String
    val transaction: IDBTransaction

    fun add(value: JsAny): IDBRequest<JsString>
    fun add(value: JsAny, key: String): IDBRequest<JsString>
    fun clear(): IDBRequest<JsAny?>
    fun count(): IDBRequest<JsNumber>
    fun count(query: String): IDBRequest<JsNumber>
    fun count(query: IDBKeyRange): IDBRequest<JsNumber>
    fun createIndex(indexName: String, keyPath: String): IDBIndex
    fun createIndex(indexName: String, keyPath: String, options: JsAny): IDBIndex
    fun delete(key: String): IDBRequest<JsAny?>
    fun delete(key: IDBKeyRange): IDBRequest<JsAny>
    fun deleteIndex(indexName: String)
    fun get(key: String): IDBRequest<JsAny>
    fun get(key: IDBKeyRange): IDBRequest<JsAny>
    fun getKey(key: String): IDBRequest<JsString>
    fun getKey(key: IDBKeyRange): IDBRequest<JsString>
    fun getAll(): IDBRequest<JsAny>
    fun getAllKeys(): IDBRequest<JsArray<JsString>>
    fun index(name: String): IDBIndex
    fun openCursor(): IDBRequest<JsAny>
    fun openKeyCursor(): IDBRequest<JsAny>
    fun put(item: JsAny): IDBRequest<JsString>
    fun put(item: JsAny, key: String): IDBRequest<JsString>
}

external class IDBTransaction : JsAny {
    val db: IDBDatabase
    val mode: String
    var oncomplete: (JsAny) -> Unit
    var onerror: (JsAny) -> Unit
    var onabort: (JsAny) -> Unit
    fun abort()
    fun commit()
    fun objectStore(name: String): IDBObjectStore
}

external class IDBIndex : JsAny {
    val name: String
    val keyPath: String
    val objectStore: IDBObjectStore
    val multiEntry: Boolean
    val unique: Boolean
}

external class IDBKeyRange : JsAny

/***
 * Helper functions
 */

suspend inline fun <T : JsAny?> IDBDatabase.suspendRequest(
    crossinline block: () -> IDBRequest<T>,
): Outcome<T, Any> = jsTryOutcome { block() }.await()

suspend fun <T : JsAny?> Outcome<IDBRequest<T>, Any>.await(): Outcome<T, Any> = unwrapOrReturn { return it }.await()

suspend fun <T : JsAny?> IDBRequest<T>.await(): Outcome<T, Any> = suspendCoroutine { continuation ->
    onsuccess = {
        jsTryOutcome {
            continuation.resume(Outcome.Ok(result))
            (0).toJsNumber()
        }.runOnError {
            continuation.resume(Outcome.Error(it))
        }
    }
    onerror = { continuation.resume(Outcome.Error(it)) }
}
