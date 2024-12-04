package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class IndexedDbWasmKmpKvStoreNode(private val name: String) : IKmpKvStoreNode {
    private var db: IDBDatabase? = null

    private val objectStoreName = "kmp_kv_store"
    private val transaction = atomic<Map<String, JsAny?>?>(null)
    private val transactionLock = reentrantLock()

    internal suspend fun open(): Outcome<Unit, Any> {
        db = indexedDB.open(name, 1)
            .await { db, oldVersion, newVersion -> db.createObjectStore(objectStoreName) }
            .unwrapOrReturn { return this }

        return Outcome.Ok(Unit)
    }

    override suspend fun close() {
        db?.close()
        db = null
    }

    override suspend fun contains(key: String): Boolean = db?.let { db ->
        db.suspendRequest {
            db.transaction(objectStoreName)
                .objectStore(objectStoreName)
                .get(key)
        }.unwrapOrNull() != null
    } == true

    override suspend fun remove(key: String): Outcome<Unit, Any> = db?.let { db ->
        recordTransaction(key)
        db.suspendRequest {
            db.transaction(objectStoreName, "readwrite")
                .objectStore(objectStoreName)
                .delete(key)
        }.unwrapOrReturn { return@let this }
        KmpKvStoreObserverRegistry.notifyValueChange(name, key, null)
        Outcome.Ok(Unit)
    } ?: Outcome.Error(IndexedDbClosedException())

    override suspend fun clear(): Outcome<Unit, Any> = db?.let { db ->
        recordClearTransaction()
        db.suspendRequest {
            db.transaction(objectStoreName, "readwrite")
                .objectStore(objectStoreName)
                .clear()
        }.unwrapOrReturn { return@let this }
        KmpKvStoreObserverRegistry.notifyClear(name)
        Outcome.Ok(Unit)
    } ?: Outcome.Error(IndexedDbClosedException())

    override suspend fun vacuum(): Outcome<Unit, Exception> = Outcome.Ok(Unit)

    override suspend fun keys(): Set<String> = db?.let { db ->
        db.suspendRequest {
            db.transaction(objectStoreName)
                .objectStore(objectStoreName)
                .getAllKeys()
        }.unwrapOrNull()?.toSet()
    } ?: emptySet()

    override suspend fun keyCount(): Long = db?.let { db ->
        db.suspendRequest {
            db.transaction(objectStoreName)
                .objectStore(objectStoreName)
                .count()
        }.unwrapOrNull()?.toInt()?.toLong()
    } ?: 0

    override suspend fun dbFileSize(): Long = 0

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun putBytes(key: String, value: ByteArray): Outcome<Unit, Any> =
        put(key) { Base64.encode(value).toJsString() }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getBytes(key: String): ByteArray? =
        get<JsString, ByteArray>(key) { Base64.decode(it.toString()) }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun observeBytes(key: String): Flow<ByteArray?> =
        observe<JsString, ByteArray>(key) { Base64.decode(it.toString()) }

    override suspend fun putBoolean(key: String, value: Boolean): Outcome<Unit, Any> = put(key) { value.toJsBoolean() }
    override suspend fun getBoolean(key: String): Boolean? = get<JsBoolean, Boolean>(key) { it.toBoolean() }
    override suspend fun observeBoolean(key: String): Flow<Boolean?> =
        observe<JsBoolean, Boolean>(key) { it.toBoolean() }

    override suspend fun putString(key: String, value: String): Outcome<Unit, Any> =
        put(key) { value.toJsString() }
    override suspend fun getString(key: String): String? = get<JsString, String>(key) { it.toString() }
    override suspend fun observeString(key: String): Flow<String?> = observe<JsString, String>(key) { it.toString() }

    override suspend fun putInt(key: String, value: Int): Outcome<Unit, Any> = put(key) { value.toJsNumber() }
    override suspend fun getInt(key: String): Int? = get<JsNumber, Int>(key) { it.toInt() }
    override suspend fun observeInt(key: String): Flow<Int?> = observe<JsNumber, Int>(key) { it.toInt() }

    override suspend fun putLong(key: String, value: Long): Outcome<Unit, Any> = put(key) { value.toJsBigInt() }
    override suspend fun getLong(key: String): Long? = get<JsBigInt, Long>(key) { it.toLong() }
    override suspend fun observeLong(key: String): Flow<Long?> = observe<JsBigInt, Long>(key) { it.toLong() }

    override suspend fun putFloat(key: String, value: Float): Outcome<Unit, Any> =
        put(key) { value.toDouble().toJsNumber() }
    override suspend fun getFloat(key: String): Float? = get<JsNumber, Float>(key) { it.toDouble().toFloat() }
    override suspend fun observeFloat(key: String): Flow<Float?> =
        observe<JsNumber, Float>(key) { it.toDouble().toFloat() }

    override suspend fun putDouble(key: String, value: Double): Outcome<Unit, Any> = put(key) { value.toJsNumber() }
    override suspend fun getDouble(key: String): Double? = get<JsNumber, Double>(key) { it.toDouble() }
    override suspend fun observeDouble(key: String): Flow<Double?> = observe<JsNumber, Double>(key) { it.toDouble() }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override suspend fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Any> = put(key) { Base64.encode(Cbor.encodeToByteArray(serializer, value)).toJsString() }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override suspend fun <T> getSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): T? = get<JsString, T>(key) { Cbor.decodeFromByteArray(deserializer, Base64.decode(it.toString())) }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override suspend fun <T> observeSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): Flow<T?> = observe<JsString, T>(key) { Cbor.decodeFromByteArray(deserializer, Base64.decode(it.toString())) }

    override suspend fun transaction(block: suspend (() -> Nothing) -> Unit) {
        transactionLock.withLock {
            try {
                transaction.update { mutableMapOf() }
                val rollback = { throw KmpKvStoreRollbackException() }
                block(rollback)
            } catch (_: Exception) {
                transaction.value?.forEach { (k, v) ->
                    if (v == null) {
                        remove(k)
                    } else {
                        when (v) {
                            is JsNumber -> put(k) { v }
                            is JsString -> put(k) { v }
                            is JsBoolean -> put(k) { v }
                            is JsBigInt -> put(k) { v }
                        }
                    }
                }
                transaction.update { null }
            }
        }
    }

    private suspend inline fun <reified T : JsAny> put(key: String, crossinline mapper: () -> T): Outcome<Unit, Any> =
        db?.let { db ->
            try {
                recordTransaction(key)
                val value = mapper()
                db.suspendRequest {
                    db.transaction(objectStoreName, "readwrite")
                        .objectStore(objectStoreName)
                        .put(key = key, item = value)
                }.unwrapOrReturn { return this }
                KmpKvStoreObserverRegistry.notifyValueChange(name, key, value)
                Outcome.Ok(Unit)
            } catch (t: Throwable) {
                Outcome.Error(t)
            }
        } ?: Outcome.Error(IndexedDbClosedException())

    private suspend fun recordClearTransaction() {
        if (transaction.value == null) return
        keys().forEach { recordTransaction(it) }
    }

    private suspend fun recordTransaction(key: String) {
        if (transaction.value == null) return
        val currentValue = getRaw(key)
        transaction.update { it?.toMutableMap()?.apply { this[key] = currentValue } }
    }

    private suspend inline fun <reified T : JsAny, R> get(
        key: String,
        crossinline mapper: (value: T) -> R?,
    ): R? = db?.let { db ->
        try {
            val value = db.suspendRequest {
                db.transaction(objectStoreName)
                    .objectStore(objectStoreName)
                    .get(key)
            }.unwrapOrNull() ?: return@let null
            if (value !is T) return@let null

            mapper(value)
        } catch (t: Throwable) {
            null
        }
    }

    private suspend inline fun getRaw(
        key: String,
    ): JsAny? = db?.let { db ->
        try {
            db.suspendRequest {
                db.transaction(objectStoreName)
                    .objectStore(objectStoreName)
                    .get(key)
            }.unwrapOrNull() ?: return@let null
        } catch (t: Throwable) {
            null
        }
    }

    private inline fun <reified T, R> observe(key: String, crossinline mapper: (rawValue: T) -> R?): Flow<R?> =
        KmpKvStoreObserverRegistry.observe<T, R>(nodeName = name, key = key, mapper)
}

class IndexedDbClosedException : Exception("IndexedDb is closed")

private fun JsArray<JsString>.toSet(): Set<String> = buildSet {
    apply {
        for (i in 0 until length) {
            get(i)?.let { add(it.toString()) }
        }
    }
}
