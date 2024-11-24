package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKVStoreDatabaseQueries
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import okio.Buffer

internal expect class KmpKVStoreContext

internal expect fun createDatabaseDriver(context: KmpKVStoreContext, nodeName: String): SqlDriver

class KmpKvStoreNode internal constructor(
    context: KmpKVStoreContext,
    private val name: String
) : IKmpKVStoreNode {
    private val driver = createDatabaseDriver(context, name)
    private val queries = KmpKVStoreDatabaseQueries(driver)

    override fun close() = driver.close()

    override fun contains(key: String): Boolean = try {
        queries.exists(key).executeAsOneOrNull() != null
    } catch (e: Exception) {
        false
    }

    override fun remove(key: String): Outcome<Unit, Exception> = try {
        queries.remove(key)
        KmpKVStoreObserverRegistry.notifyValueChange(name, key, null)
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override fun clear(): Outcome<Unit, Exception> = try {
        queries.clear()
        KmpKVStoreObserverRegistry.notifyClear(name)
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override fun vacuum(): Outcome<Unit, Exception> = try {
        queries.vacuum()
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override fun getKeys(): Set<String> = try {
        queries.getKeys().executeAsList().toSet()
    } catch (e: Exception) {
        emptySet()
    }

    override fun keyCount(): Long = try {
        queries.getKeyCount().executeAsOne()
    } catch (e: Exception) {
        0
    }

    override fun dbFileSize(): Long = try {
        driver.executeQuery(
            identifier = null,
            sql = "SELECT `page_count` * `page_size` as `size` FROM pragma_page_count(), pragma_page_size();",
            mapper = { QueryResult.Value(it.getLong(0)) },
            parameters = 0,
        ).value ?: 0
    } catch (e: Exception) {
        0
    }

    override fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> =
        put(key) { value }

    override fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> =
        put(key) { byteArrayOf(if (value) 0x01 else 0x00) }

    override fun putString(key: String, value: String): Outcome<Unit, Exception> =
        put(key) { value.encodeToByteArray() }

    override fun putInt(key: String, value: Int): Outcome<Unit, Exception> =
        put(key) { Buffer().writeInt(value).readByteArray() }

    override fun putLong(key: String, value: Long): Outcome<Unit, Exception> =
        put(key) { Buffer().writeLong(value).readByteArray() }

    override fun putFloat(key: String, value: Float): Outcome<Unit, Exception> =
        put(key) { Buffer().writeInt(value.toBits()).readByteArray() }

    override fun putDouble(key: String, value: Double): Outcome<Unit, Exception> =
        put(key) { Buffer().writeLong(value.toBits()).readByteArray() }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> = put(key) { Cbor.encodeToByteArray(serializer, value) }

    override fun getBytes(key: String): ByteArray? = get(key) { it }

    override fun observeBytes(key: String): Flow<ByteArray?> = observe(key) { it }

    override fun getBoolean(key: String): Boolean? = get(key) { it[0] == 0x01.toByte() }

    override fun observeBoolean(key: String): Flow<Boolean?> = observe(key) { it[0] == 0x01.toByte() }

    override fun getString(key: String): String? = get(key) { Buffer().write(it).readUtf8() }

    override fun observeString(key: String): Flow<String?> = observe(key) { Buffer().write(it).readUtf8() }

    override fun getInt(key: String): Int? = get(key) { Buffer().write(it).readInt() }

    override fun observeInt(key: String): Flow<Int?> = observe(key) { Buffer().write(it).readInt() }

    override fun getLong(key: String): Long? = get(key) { Buffer().write(it).readLong() }

    override fun observeLong(key: String): Flow<Long?> = observe(key) { Buffer().write(it).readLong() }

    override fun getFloat(key: String): Float? = get(key) { Float.fromBits(Buffer().write(it).readInt()) }

    override fun observeFloat(key: String): Flow<Float?> = observe(key) { Float.fromBits(Buffer().write(it).readInt()) }

    override fun getDouble(key: String): Double? = get(key) { Double.fromBits(Buffer().write(it).readLong()) }

    override fun observeDouble(key: String): Flow<Double?> =
        observe(key) { Double.fromBits(Buffer().write(it).readLong()) }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? =
        get(key) { Cbor.decodeFromByteArray(deserializer, queries.get(key).executeAsOne()) }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T?> =
        observe(key) { Cbor.decodeFromByteArray(deserializer, it) }

    override fun transaction(block: (rollback: () -> Nothing) -> Unit) {
        queries.transaction {
            block(::rollback)
        }
    }

    private inline fun <T> get(key: String, crossinline mapper: (bytes: ByteArray) -> T): T? {
        return try {
            return mapper(queries.get(key).executeAsOne())
        } catch (e: Exception) {
            null
        }
    }

    private inline fun put(key: String, crossinline mapper: () -> ByteArray): Outcome<Unit, Exception> {
        return try {
            val value = mapper()
            queries.put(key, value)
            KmpKVStoreObserverRegistry.notifyValueChange(name, key, value)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private inline fun <T> observe(key: String, crossinline mapper: (bytes: ByteArray) -> T?): Flow<T?> = channelFlow {
        try {
            val listener: KmpKVStoreObserver = listener@{ value: Any? ->
                if (value !is ByteArray?) return@listener
                if (value == null) {
                    send(null)
                    return@listener
                }
                val mappedValue = mapper(value) ?: return@listener
                send(mappedValue)
            }
            KmpKVStoreObserverRegistry.addListener(name, key, listener)
            awaitClose { KmpKVStoreObserverRegistry.removeListener(name, key, listener) }
        } catch (_: Exception) {
            // NoOp
        }
    }.distinctUntilChanged()
}
