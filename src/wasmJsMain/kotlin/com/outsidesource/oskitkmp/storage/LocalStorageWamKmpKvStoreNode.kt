package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class LocalStorageWasmKmpKvStoreNode(val name: String) : IKmpKvStoreNode {

    private val transaction = atomic<Map<String, String?>?>(null)
    private val transactionLock = reentrantLock()

    override suspend fun close() {}

    override suspend fun contains(key: String): Boolean = localStorage.getItem(normalizeKey(key)) != null

    override suspend fun remove(key: String): Outcome<Unit, Exception> {
        recordTransaction(key)
        localStorage.removeItem(normalizeKey(key))
        KmpKvStoreObserverRegistry.notifyValueChange(name, key, null)
        return Outcome.Ok(Unit)
    }

    override suspend fun clear(): Outcome<Unit, Exception> {
        keys().forEach {
            recordTransaction(it)
            localStorage.removeItem(normalizeKey(it))
            KmpKvStoreObserverRegistry.notifyValueChange(name, it, null)
        }
        return Outcome.Ok(Unit)
    }

    override suspend fun vacuum(): Outcome<Unit, Exception> = Outcome.Ok(Unit)

    override suspend fun keys(): Set<String> = buildSet {
        for (i in 0 until localStorage.length) {
            val keyName = localStorage.key(i) ?: continue
            if (!keyName.startsWith(normalizedNodeName())) continue
            add(keyName.removePrefix(normalizedNodeName()))
        }
    }

    override suspend fun keyCount(): Long = keys().size.toLong()

    override suspend fun dbFileSize(): Long = 0

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> = put(key) {
        Base64.encode(
            value,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getBytes(key: String): ByteArray? = get(key) { Base64.decode(it) }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun observeBytes(key: String): Flow<ByteArray?> = observe(key) { Base64.decode(it) }

    override suspend fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> =
        put(key) { value.toString() }
    override suspend fun getBoolean(key: String): Boolean? = get(key) { it.toBooleanStrictOrNull() }
    override suspend fun observeBoolean(key: String): Flow<Boolean?> = observe(key) { it.toBooleanStrictOrNull() }

    override suspend fun putString(key: String, value: String): Outcome<Unit, Exception> = put(key) { value.toString() }
    override suspend fun getString(key: String): String? = get(key) { it }
    override suspend fun observeString(key: String): Flow<String?> = observe(key) { it }

    override suspend fun putInt(key: String, value: Int): Outcome<Unit, Exception> = put(key) { value.toString() }
    override suspend fun getInt(key: String): Int? = get(key) { it.toIntOrNull() }
    override suspend fun observeInt(key: String): Flow<Int?> = observe(key) { it.toIntOrNull() }

    override suspend fun putLong(key: String, value: Long): Outcome<Unit, Exception> = put(key) { value.toString() }
    override suspend fun getLong(key: String): Long? = get(key) { it.toLongOrNull() }
    override suspend fun observeLong(key: String): Flow<Long?> = observe(key) { it.toLongOrNull() }

    override suspend fun putFloat(key: String, value: Float): Outcome<Unit, Exception> = put(key) { value.toString() }
    override suspend fun getFloat(key: String): Float? = get(key) { it.toFloatOrNull() }
    override suspend fun observeFloat(key: String): Flow<Float?> = observe(key) { it.toFloatOrNull() }

    override suspend fun putDouble(key: String, value: Double): Outcome<Unit, Exception> = put(key) { value.toString() }
    override suspend fun getDouble(key: String): Double? = get(key) { it.toDoubleOrNull() }
    override suspend fun observeDouble(key: String): Flow<Double?> = observe(key) { it.toDoubleOrNull() }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override suspend fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> = put(key) { Base64.encode(Cbor.encodeToByteArray(serializer, value)) }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override suspend fun <T> getSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): T? = get(key) { Cbor.decodeFromByteArray(deserializer, Base64.decode(it)) }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)
    override suspend fun <T> observeSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): Flow<T?> = observe(key) { Cbor.decodeFromByteArray(deserializer, Base64.decode(it)) }

    override suspend fun transaction(block: suspend (rollback: () -> Nothing) -> Unit) {
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
                        put(k) { v }
                    }
                }
                transaction.update { null }
            }
        }
    }

    private inline fun put(key: String, crossinline mapper: () -> String): Outcome<Unit, Exception> {
        return try {
            recordTransaction(key)
            val value = mapper()
            localStorage.setItem(normalizeKey(key), value)
            KmpKvStoreObserverRegistry.notifyValueChange(name, key, value)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private fun recordTransaction(key: String) {
        if (transaction.value == null) return
        val currentValue = localStorage.getItem(normalizeKey(key))
        transaction.update { it?.toMutableMap()?.apply { put(key, currentValue) } }
    }

    private inline fun <T> get(key: String, crossinline mapper: (value: String) -> T): T? {
        return try {
            val value = localStorage.getItem(normalizeKey(key)) ?: return null
            return mapper(value)
        } catch (_: Exception) {
            null
        }
    }

    private inline fun <T> observe(key: String, crossinline mapper: (rawValue: String) -> T?): Flow<T?> =
        KmpKvStoreObserverRegistry.observe<String, T>(nodeName = name, key = key, mapper)

    fun normalizeKey(key: String) = "${normalizedNodeName()}$key"
    fun normalizedNodeName() = "__${name}__:"
}
