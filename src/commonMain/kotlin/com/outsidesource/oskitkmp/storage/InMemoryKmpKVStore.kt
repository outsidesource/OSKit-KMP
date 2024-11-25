package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor

class InMemoryKmpKVStore : IKmpKVStore {
    private val lock = SynchronizedObject()
    private val nodes = mutableMapOf<String, IKmpKVStoreNode>()

    override suspend fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> {
        synchronized(lock) {
            if (!nodes.contains(nodeName)) nodes[nodeName] = InMemoryKmpKVStoreNode(nodeName)
            return Outcome.Ok(nodes[nodeName]!!)
        }
    }
}

/**
 * [InMemoryKmpKVStoreNode] provides a thread-safe, fallback, in-memory storage node
 */
class InMemoryKmpKVStoreNode(
    private val name: String,
) : IKmpKVStoreNode {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val storage = atomic(mapOf<String, Any>())
    private val transaction = atomic<Map<String, Any?>?>(null)
    private val transactionLock = reentrantLock()

    override suspend fun clear(): Outcome<Unit, Exception> {
        storage.update { it.toMutableMap().apply { clear() } }
        KmpKVStoreObserverRegistry.notifyClear(name)
        return Outcome.Ok(Unit)
    }

    override suspend fun close() = scope.cancel()
    override suspend fun contains(key: String): Boolean = storage.value.containsKey(key)
    override suspend fun dbFileSize(): Long = 0
    override suspend fun keyCount(): Long = storage.value.keys.size.toLong()
    override suspend fun keys(): Set<String> = storage.value.keys
    override suspend fun vacuum(): Outcome<Unit, Exception> = Outcome.Ok(Unit)
    override suspend fun remove(key: String): Outcome<Unit, Exception> = put(key, null)

    override suspend fun getBoolean(key: String): Boolean? = storage.value[key] as? Boolean
    override suspend fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> = put(key, value)
    override suspend fun observeBoolean(key: String): Flow<Boolean?> = observe(key)

    override suspend fun getBytes(key: String): ByteArray? = storage.value[key] as? ByteArray
    override suspend fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> = put(key, value)
    override suspend fun observeBytes(key: String): Flow<ByteArray?> = observe(key)

    override suspend fun getDouble(key: String): Double? = storage.value[key] as? Double
    override suspend fun putDouble(key: String, value: Double): Outcome<Unit, Exception> = put(key, value)
    override suspend fun observeDouble(key: String): Flow<Double?> = observe(key)

    override suspend fun getFloat(key: String): Float? = storage.value[key] as? Float
    override suspend fun putFloat(key: String, value: Float): Outcome<Unit, Exception> = put(key, value)
    override suspend fun observeFloat(key: String): Flow<Float?> = observe(key)

    override suspend fun getInt(key: String): Int? = storage.value[key] as? Int
    override suspend fun putInt(key: String, value: Int): Outcome<Unit, Exception> = put(key, value)
    override suspend fun observeInt(key: String): Flow<Int?> = observe(key)

    override suspend fun putLong(key: String, value: Long): Outcome<Unit, Exception> = put(key, value)
    override suspend fun getLong(key: String): Long? = storage.value[key] as? Long
    override suspend fun observeLong(key: String): Flow<Long?> = observe(key)

    override suspend fun putString(key: String, value: String): Outcome<Unit, Exception> = put(key, value)
    override suspend fun getString(key: String): String? = storage.value[key] as? String
    override suspend fun observeString(key: String): Flow<String?> = observe(key)

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> {
        return try {
            put(key, Cbor.encodeToByteArray(serializer, value))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? {
        return try {
            Cbor.decodeFromByteArray(deserializer, storage.value[key] as? ByteArray ?: return null)
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T?> =
        observe<ByteArray?>(key).map {
            if (it == null) return@map null
            try {
                Cbor.decodeFromByteArray(deserializer, it)
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun transaction(block: suspend (rollback: () -> Nothing) -> Unit) {
        transactionLock.withLock {
            try {
                transaction.update { mutableMapOf() }
                val rollback = { throw KmpKVStoreRollbackException() }
                block(rollback)
            } catch (_: Exception) {
                transaction.value?.forEach { (k, v) ->
                    if (v == null) {
                        this@InMemoryKmpKVStoreNode.remove(k)
                    } else {
                        this@InMemoryKmpKVStoreNode.put(k, v)
                    }
                }
                transaction.update { null }
            }
        }
    }

    private fun put(key: String, value: Any?): Outcome<Unit, Exception> {
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

        KmpKVStoreObserverRegistry.notifyValueChange(name, key, value)
        return Outcome.Ok(Unit)
    }

    private inline fun <reified T> observe(key: String): Flow<T?> =
        KmpKVStoreObserverRegistry.observe<T, T>(nodeName = name, key = key) { it }
}
