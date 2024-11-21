package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.tuples.Tup2
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor

class InMemoryKMPStorage : IKMPStorage {
    private val lock = SynchronizedObject()
    private val nodes = mutableMapOf<String, IKMPStorageNode>()

    override fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception> {
        synchronized(lock) {
            if (!nodes.contains(nodeName)) nodes[nodeName] = InMemoryKMPStorageNode()
            return Outcome.Ok(nodes[nodeName]!!)
        }
    }
}

/**
 * InMemoryKMPStorageNode provides a thread-safe, fallback, in-memory storage node
 */
class InMemoryKMPStorageNode : IKMPStorageNode {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val storage = atomic(mapOf<String, Any>())
    private val transaction = atomic<Map<String, Any?>?>(null)
    private val transactionLock = reentrantLock()
    private val observer = MutableSharedFlow<Tup2<String, Any>>()

    override fun clear(): Outcome<Unit, Exception> {
        storage.update { it.toMutableMap().apply { clear() } }
        return Outcome.Ok(Unit)
    }

    override fun close() = scope.cancel()
    override fun contains(key: String): Boolean = storage.value.containsKey(key)
    override fun dbFileSize(): Long = 0
    override fun keyCount(): Long = storage.value.keys.size.toLong()

    override fun getBoolean(key: String): Boolean? = storage.value[key] as? Boolean
    override fun getBytes(key: String): ByteArray? = storage.value[key] as? ByteArray
    override fun getDouble(key: String): Double? = storage.value[key] as? Double
    override fun getFloat(key: String): Float? = storage.value[key] as? Float
    override fun getInt(key: String): Int? = storage.value[key] as? Int
    override fun getKeys(): List<String> = storage.value.keys.toList()
    override fun getLong(key: String): Long? = storage.value[key] as? Long
    override fun getString(key: String): String? = storage.value[key] as? String

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? {
        return try {
            Cbor.decodeFromByteArray(deserializer, storage.value[key] as? ByteArray ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    override fun observeBoolean(key: String): Flow<Boolean> = observe(key)
    override fun observeBytes(key: String): Flow<ByteArray> = observe(key)
    override fun observeDouble(key: String): Flow<Double> = observe(key)
    override fun observeFloat(key: String): Flow<Float> = observe(key)
    override fun observeInt(key: String): Flow<Int> = observe(key)
    override fun observeLong(key: String): Flow<Long> = observe(key)
    override fun observeString(key: String): Flow<String> = observe(key)

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T> =
        observe<ByteArray>(key).mapNotNull {
            try {
                Cbor.decodeFromByteArray(deserializer, it)
            } catch (e: Exception) {
                null
            }
        }

    override fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> = performWrite(key, value)
    override fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> = performWrite(key, value)
    override fun putDouble(key: String, value: Double): Outcome<Unit, Exception> = performWrite(key, value)
    override fun putFloat(key: String, value: Float): Outcome<Unit, Exception> = performWrite(key, value)
    override fun putInt(key: String, value: Int): Outcome<Unit, Exception> = performWrite(key, value)
    override fun putLong(key: String, value: Long): Outcome<Unit, Exception> = performWrite(key, value)
    override fun putString(key: String, value: String): Outcome<Unit, Exception> = performWrite(key, value)
    override fun remove(key: String): Outcome<Unit, Exception> = performWrite(key, null)

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> {
        return try {
            performWrite(key, Cbor.encodeToByteArray(serializer, value))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override fun transaction(block: (rollback: () -> Nothing) -> Unit) {
        transactionLock.withLock {
            try {
                transaction.update { mutableMapOf() }
                val rollback = { throw KMPStorageRollbackException() }
                block(rollback)
            } catch (e: Exception) {
                storage.update {
                    it.toMutableMap().apply {
                        transaction.value?.forEach { (k, v) ->
                            if (v == null) {
                                remove(k)
                            } else {
                                put(k, v)
                            }
                        }
                    }
                }
                transaction.update { null }
            }
        }
    }

    override fun vacuum(): Outcome<Unit, Exception> = Outcome.Ok(Unit)

    private fun performWrite(key: String, value: Any?): Outcome<Unit, Exception> {
        if (transaction.value != null) transaction.update { it?.toMutableMap()?.apply { put(key, storage.value[key]) } }
        storage.update {
            it.toMutableMap().apply {
                if (value == null) {
                    remove(key)
                } else {
                    put(key, value)
                }
            }
        }

        if (value != null) scope.launch { observer.emit(Tup2(key, value)) }
        return Outcome.Ok(Unit)
    }

    private inline fun <reified T> observe(key: String): Flow<T> =
        observer.filter { it.v1 == key }.mapNotNull { it.v2 as? T }
}

private class KMPStorageRollbackException : Exception("Transaction Rolled Back")
