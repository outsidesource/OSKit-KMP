package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.locks.withLock
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor

class InMemoryKmpKVStore : IKmpKVStore {
    private val lock = SynchronizedObject()
    private val nodes = mutableMapOf<String, IKmpKVStoreNode>()

    override fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Exception> {
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
    private val name: String
) : IKmpKVStoreNode {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val storage = atomic(mapOf<String, Any>())
    private val transaction = atomic<Map<String, Any?>?>(null)
    private val transactionLock = reentrantLock()

    override fun clear(): Outcome<Unit, Exception> {
        storage.update { it.toMutableMap().apply { clear() } }
        KmpKVStoreObserverRegistry.notifyClear(name)
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
    override fun getKeys(): Set<String> = storage.value.keys
    override fun getLong(key: String): Long? = storage.value[key] as? Long
    override fun getString(key: String): String? = storage.value[key] as? String

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? {
        return try {
            Cbor.decodeFromByteArray(deserializer, storage.value[key] as? ByteArray ?: return null)
        } catch (_: Exception) {
            null
        }
    }

    override fun observeBoolean(key: String): Flow<Boolean?> = observe(key)
    override fun observeBytes(key: String): Flow<ByteArray?> = observe(key)
    override fun observeDouble(key: String): Flow<Double?> = observe(key)
    override fun observeFloat(key: String): Flow<Float?> = observe(key)
    override fun observeInt(key: String): Flow<Int?> = observe(key)
    override fun observeLong(key: String): Flow<Long?> = observe(key)
    override fun observeString(key: String): Flow<String?> = observe(key)

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T?> =
        observe<ByteArray?>(key).map {
            if (it == null) return@map null
            try {
                Cbor.decodeFromByteArray(deserializer, it)
            } catch (_: Exception) {
                null
            }
        }

    override fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> = put(key, value)
    override fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> = put(key, value)
    override fun putDouble(key: String, value: Double): Outcome<Unit, Exception> = put(key, value)
    override fun putFloat(key: String, value: Float): Outcome<Unit, Exception> = put(key, value)
    override fun putInt(key: String, value: Int): Outcome<Unit, Exception> = put(key, value)
    override fun putLong(key: String, value: Long): Outcome<Unit, Exception> = put(key, value)
    override fun putString(key: String, value: String): Outcome<Unit, Exception> = put(key, value)
    override fun remove(key: String): Outcome<Unit, Exception> = put(key, null)

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> putSerializable(
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

    override fun transaction(block: (rollback: () -> Nothing) -> Unit) {
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

    override fun vacuum(): Outcome<Unit, Exception> = Outcome.Ok(Unit)

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

    private inline fun <reified T> observe(key: String): Flow<T?> = channelFlow {
        try {
            val listener: KmpKVStoreObserver = listener@{ value: Any? ->
                if (value !is T?) return@listener
                send(value)
            }
            KmpKVStoreObserverRegistry.addListener(name, key, listener)
            awaitClose { KmpKVStoreObserverRegistry.removeListener(name, key, listener) }
        } catch (_: Exception) {
            // NoOp
        }
    }.distinctUntilChanged()
}
