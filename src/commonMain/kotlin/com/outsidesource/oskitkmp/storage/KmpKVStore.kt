package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

/**
 * [KmpKVStore] is a multiplatform key value store that allows persistent storage of any data
 *
 * All [KmpKVStore] and [KmpKVStoreNode] methods are blocking and should be run in a coroutine on
 * [Dispatchers.IO]
 *
 * To use [KmpKVStore] create an instance of each platform independent implementation, [AndroidKmpKVStore],
 * [IosKmpKVStore], [JvmKmpKVStore], [WasmKmpKVStore]. Each implementation implements [IKmpKVStore].
 *
 * Desktop/JVM:
 * You may need to add the `java.sql` module:
 * ```
 * compose.desktop {
 *    nativeDistributions {
 *        modules("java.sql")
 *    }
 * }
 * ```
 *
 * iOS:
 * You may need to add the linker flag `-lsqlite3`
 *
 * WASM:
 * Using LocalStorage has a limited amount of storage (5MiB). IndexedDB can use more storage but the exact amount
 * depends on the browser.
 * https://developer.mozilla.org/en-US/docs/Web/API/Storage_API/Storage_quotas_and_eviction_criteria
 */
interface IKmpKVStore {
    suspend fun openNode(nodeName: String): Outcome<IKmpKVStoreNode, Any>
}

interface IKmpKVStoreNode {
    fun createUniqueKey(): String = KmpKVStoreKeyGenerator.createUniqueKey()

    suspend fun close()
    suspend fun contains(key: String): Boolean
    suspend fun remove(key: String): Outcome<Unit, Any>
    suspend fun clear(): Outcome<Unit, Any>
    suspend fun vacuum(): Outcome<Unit, Any>
    suspend fun keys(): Set<String>
    suspend fun keyCount(): Long
    suspend fun dbFileSize(): Long

    suspend fun putBytes(key: String, value: ByteArray): Outcome<Unit, Any>
    suspend fun getBytes(key: String): ByteArray?
    suspend fun observeBytes(key: String): Flow<ByteArray?>

    suspend fun putBoolean(key: String, value: Boolean): Outcome<Unit, Any>
    suspend fun getBoolean(key: String): Boolean?
    suspend fun observeBoolean(key: String): Flow<Boolean?>

    suspend fun putString(key: String, value: String): Outcome<Unit, Any>
    suspend fun getString(key: String): String?
    suspend fun observeString(key: String): Flow<String?>

    suspend fun putInt(key: String, value: Int): Outcome<Unit, Any>
    suspend fun getInt(key: String): Int?
    suspend fun observeInt(key: String): Flow<Int?>

    suspend fun putLong(key: String, value: Long): Outcome<Unit, Any>
    suspend fun getLong(key: String): Long?
    suspend fun observeLong(key: String): Flow<Long?>

    suspend fun putFloat(key: String, value: Float): Outcome<Unit, Any>
    suspend fun getFloat(key: String): Float?
    suspend fun observeFloat(key: String): Flow<Float?>

    suspend fun putDouble(key: String, value: Double): Outcome<Unit, Any>
    suspend fun getDouble(key: String): Double?
    suspend fun observeDouble(key: String): Flow<Double?>

    suspend fun <T> putSerializable(key: String, value: T, serializer: SerializationStrategy<T>): Outcome<Unit, Any>
    suspend fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T?
    suspend fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T?>

    suspend fun transaction(block: suspend (rollback: () -> Nothing) -> Unit)
}

internal class KmpKVStoreRollbackException : Exception("Transaction Rolled Back")

internal typealias KmpKVStoreObserver = suspend (Any?) -> Unit

internal object KmpKVStoreKeyGenerator {
    private val counter = atomic(Random(Clock.System.now().toEpochMilliseconds()).nextLong())

    @OptIn(ExperimentalStdlibApi::class)
    fun createUniqueKey(): String =
        Clock.System.now().epochSeconds.toHexString().takeLast(10) + counter.incrementAndGet().toHexString()
}

internal object KmpKVStoreObserverRegistry {
    private data class ValueListenerContext(
        val coroutineContext: CoroutineContext = Dispatchers.Default.limitedParallelism(1),
        val listeners: List<KmpKVStoreObserver> = emptyList(),
    )

    private val scope = CoroutineScope(Dispatchers.Default)
    private val listeners: AtomicRef<Map<String, Map<String, ValueListenerContext>>> = atomic(emptyMap())

    private fun addListener(node: String, key: String, listener: KmpKVStoreObserver) = listeners.update {
        it.toMutableMap().apply {
            this[node] = (this[node] ?: emptyMap()).toMutableMap().apply {
                val context = (this[key] ?: ValueListenerContext()).let { it.copy(listeners = it.listeners + listener) }
                this[key] = context
            }
        }
    }

    private fun removeListener(node: String, key: String, listener: KmpKVStoreObserver) = listeners.update {
        it.toMutableMap().apply {
            this[node] = (this[node] ?: emptyMap()).toMutableMap().apply {
                val listeners = (this[key] ?: ValueListenerContext())
                    .let { it.copy(listeners = it.listeners - listener) }
                this[key] = listeners
            }
        }
    }

    fun notifyValueChange(node: String, key: String, value: Any?) {
        val context = listeners.value[node]?.get(key) ?: return
        scope.launch(context.coroutineContext) {
            context.listeners.forEach { it(value) }
        }
    }

    fun notifyClear(node: String) {
        scope.launch {
            listeners.value[node]?.forEach { keyMapEntry ->
                val context = keyMapEntry.value
                scope.launch(context.coroutineContext) {
                    context.listeners.forEach { it(null) }
                }
            }
        }
    }

    inline fun <reified V, R> observe(
        nodeName: String,
        key: String,
        crossinline mapper: (rawValue: V) -> R?,
    ): Flow<R?> = channelFlow {
        try {
            val listener: KmpKVStoreObserver = listener@{ value: Any? ->
                if (value !is V?) return@listener
                if (value == null) {
                    send(null)
                    return@listener
                }
                val mappedValue = mapper(value) ?: return@listener
                send(mappedValue)
            }
            addListener(nodeName, key, listener)
            awaitClose { removeListener(nodeName, key, listener) }
        } catch (_: Exception) {
            // NoOp
        }
    }.distinctUntilChanged()
}
