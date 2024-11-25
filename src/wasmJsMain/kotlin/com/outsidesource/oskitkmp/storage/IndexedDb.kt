package com.outsidesource.oskitkmp.storage

external object indexedDB {
    fun open(name: String): IDBOpenDBRequest
    fun open(name: String, version: Int): IDBOpenDBRequest
}

external class IDBOpenDBRequest : JsAny {
    var onerror: (Event) -> Unit
    var onsuccess: (IDBOpenSuccessEvent) -> Unit
    var onupgradeneeded: (IDBVersionChangeEvent) -> Unit
    val result: IDBDatabase
}

open external class IDBRequest : JsAny {
    val error: JsAny
    val readyState: JsAny
    val result: JsAny
    val source: JsAny
    val transaction: JsAny
    var onerror: (Event) -> Unit
    var onsuccess: (Event) -> Unit
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

    fun add(value: JsAny): IDBRequest
    fun add(value: JsAny, key: String): IDBRequest
    fun clear(): IDBRequest
    fun count(): IDBRequest
    fun count(query: String): IDBRequest
    fun count(query: IDBKeyRange): IDBRequest
    fun createIndex(indexName: String, keyPath: String): IDBIndex
    fun createIndex(indexName: String, keyPath: String, options: JsAny): IDBIndex
    fun delete(key: String): IDBRequest
    fun delete(key: IDBKeyRange): IDBRequest
    fun deleteIndex(indexName: String)
    fun get(key: String): IDBRequest
    fun get(key: IDBKeyRange): IDBRequest
    fun getKey(key: String): IDBRequest
    fun getKey(key: IDBKeyRange): IDBRequest
    fun getAll(): IDBRequest
    fun getAllKeys(): IDBRequest
    fun index(name: String): IDBIndex
    fun openCursor(): IDBRequest
    fun openKeyCursor(): IDBRequest
    fun put(item: JsAny): IDBRequest
    fun put(item: JsAny, key: String): IDBRequest
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

external class IDBIndex : JsAny

external class IDBKeyRange : JsAny
