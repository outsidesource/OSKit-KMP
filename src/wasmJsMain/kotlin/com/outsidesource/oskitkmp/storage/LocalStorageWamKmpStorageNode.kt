package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.browser.localStorage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class LocalStorageWasmKmpStorageNode(val nodeName: String) : IKMPStorageNode {

    private val transaction = atomic<Map<String, String?>?>(null)
    private val transactionLock = reentrantLock()

    override fun close() {}

    override fun contains(key: String): Boolean {
        return localStorage.getItem(normalizeKey(key)) != null
    }

    override fun remove(key: String): Outcome<Unit, Exception> {
        localStorage.removeItem(normalizeKey(key))
        return Outcome.Ok(Unit)
    }

    override fun clear(): Outcome<Unit, Exception> {
        localStorage.clear()
        return Outcome.Ok(Unit)
    }

    override fun vacuum(): Outcome<Unit, Exception> {
        return Outcome.Ok(Unit)
    }

    override fun getKeys(): Set<String> {
        return buildSet {
            for (i in 0 until localStorage.length) {
                val keyName = localStorage.key(i) ?: continue
                if (!keyName.startsWith("$nodeName:")) continue
                add(keyName.removePrefix("$nodeName:"))
            }
        }
    }

    override fun keyCount(): Long = getKeys().size.toLong()

    override fun dbFileSize(): Long = 0

    @OptIn(ExperimentalEncodingApi::class)
    override fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> = put(key) { Base64.encode(value) }

    @OptIn(ExperimentalEncodingApi::class)
    override fun getBytes(key: String): ByteArray? = get(key) { Base64.decode(it) }

    @OptIn(ExperimentalEncodingApi::class)
    override fun observeBytes(key: String): Flow<ByteArray> = observe(key) { Base64.decode(it) }

    override fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> = put(key) { value.toString() }
    override fun getBoolean(key: String): Boolean? = get(key) { it.toBooleanStrictOrNull() }
    override fun observeBoolean(key: String): Flow<Boolean> = observe(key) { it.toBooleanStrictOrNull() }

    override fun putString(key: String, value: String): Outcome<Unit, Exception> = put(key) { value.toString() }
    override fun getString(key: String): String? = get(key) { it }
    override fun observeString(key: String): Flow<String> = observe(key) { it }

    override fun putInt(key: String, value: Int): Outcome<Unit, Exception> = put(key) { value.toString() }
    override fun getInt(key: String): Int? = get(key) { it.toIntOrNull() }
    override fun observeInt(key: String): Flow<Int> = observe(key) { it.toIntOrNull() }

    override fun putLong(key: String, value: Long): Outcome<Unit, Exception> = put(key) { value.toString() }
    override fun getLong(key: String): Long? = get(key) { it.toLongOrNull() }
    override fun observeLong(key: String): Flow<Long> = observe(key) { it.toLongOrNull() }

    override fun putFloat(key: String, value: Float): Outcome<Unit, Exception> = put(key) { value.toString() }
    override fun getFloat(key: String): Float? = get(key) { it.toFloatOrNull() }
    override fun observeFloat(key: String): Flow<Float> = observe(key) { it.toFloatOrNull() }

    override fun putDouble(key: String, value: Double): Outcome<Unit, Exception> = put(key) { value.toString() }
    override fun getDouble(key: String): Double? = get(key) { it.toDoubleOrNull() }
    override fun observeDouble(key: String): Flow<Double> = observe(key) { it.toDoubleOrNull() }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> = put(key) { Base64.encode(Cbor.encodeToByteArray(serializer, value)) }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override fun <T> getSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): T? = get(key) { Cbor.decodeFromByteArray(deserializer, Base64.decode(it)) }

    @OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)
    override fun <T> observeSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): Flow<T> = observe(key) { Cbor.decodeFromByteArray(deserializer, Base64.decode(it)) }

    override fun transaction(block: (rollback: () -> Nothing) -> Unit) {
        transactionLock.withLock {
            try {
                transaction.update { mutableMapOf() }
                val rollback = { throw KMPStorageRollbackException() }
                block(rollback)
            } catch (_: Exception) {
                transaction.value?.forEach { (k, v) ->
                    if (v == null) {
                        remove(k)
                    } else {
                        put(k) { v }
                    }
                    if (v != null) WasmQueryRegistry.notifyListeners(k)
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
            if (transaction.value == null) WasmQueryRegistry.notifyListeners(key)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private fun recordTransaction(key: String) {
        if (transaction.value == null) return
        val currentValue = get(normalizeKey(key)) { it }
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

    private inline fun <T> observe(key: String, crossinline mapper: (bytes: String) -> T?): Flow<T> = channelFlow {
        try {
            val normalizedKey = normalizeKey(key)
            val listener = listener@{
                val value = get(normalizedKey, mapper) ?: return@listener
                trySend(value)
            }
            WasmQueryRegistry.addListener(normalizedKey, listener)
            awaitClose { WasmQueryRegistry.removeListener(normalizedKey, listener) }
        } catch (_: Exception) {
            // NoOp
        }
    }.buffer(1, BufferOverflow.DROP_OLDEST)

    private fun normalizeKey(key: String) = "__${nodeName}__:$key"
}

private class KMPStorageRollbackException : Exception("Transaction Rolled Back")
