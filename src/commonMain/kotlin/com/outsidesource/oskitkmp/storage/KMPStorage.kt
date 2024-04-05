package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import okio.Buffer

/**
 * [KMPStorage] is a multiplatform key value store that allows persistent storage of any data
 *
 * All [KMPStorage] and [KMPStorageNode] methods are blocking and should be run in a coroutine on
 * [Dispatchers.IO]
 *
 * To use [KMPStorage] create an instance of each platform independent implementation, [AndroidKMPStorage],
 * [IOSKMPStorage], [DesktopKMPStorage] (JVM). Each implementation implements [IKMPStorage].
 */
interface IKMPStorage {
    fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception>
}

internal expect class KMPStorageContext

internal expect fun createDatabaseDriver(context: KMPStorageContext, nodeName: String): SqlDriver

interface IKMPStorageNode {
    fun close()
    fun contains(key: String): Boolean
    fun remove(key: String): Outcome<Unit, Exception>
    fun clear(): Outcome<Unit, Exception>
    fun vacuum(): Outcome<Unit, Exception>
    fun getKeys(): List<String>?
    fun keyCount(): Long
    fun dbFileSize(): Long
    fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception>
    fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception>
    fun putString(key: String, value: String): Outcome<Unit, Exception>
    fun putInt(key: String, value: Int): Outcome<Unit, Exception>
    fun putLong(key: String, value: Long): Outcome<Unit, Exception>
    fun putFloat(key: String, value: Float): Outcome<Unit, Exception>
    fun putDouble(key: String, value: Double): Outcome<Unit, Exception>
    fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception>
    fun getBytes(key: String): ByteArray?
    fun observeBytes(key: String): Flow<ByteArray>
    fun getBoolean(key: String): Boolean?
    fun observeBoolean(key: String): Flow<Boolean>
    fun getString(key: String): String?
    fun observeString(key: String): Flow<String>
    fun getInt(key: String): Int?
    fun observeInt(key: String): Flow<Int>
    fun getLong(key: String): Long?
    fun observeLong(key: String): Flow<Long>
    fun getFloat(key: String): Float?
    fun observeFloat(key: String): Flow<Float>
    fun getDouble(key: String): Double?
    fun observeDouble(key: String): Flow<Double>
    fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T?
    fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T>
    fun transaction(block: (rollback: () -> Nothing) -> Unit)
}

class KMPStorageNode internal constructor(context: KMPStorageContext, name: String) : IKMPStorageNode {
    private val driver = createDatabaseDriver(context, name)
    private val queries = KMPStorageDatabaseQueries(driver)

    override fun close() = driver.close()

    override fun contains(key: String): Boolean = try {
        queries.exists(key).executeAsOneOrNull() != null
    } catch (e: Exception) {
        false
    }

    override fun remove(key: String): Outcome<Unit, Exception> = try {
        queries.remove(key)
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override fun clear(): Outcome<Unit, Exception> = try {
        queries.clear()
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

    override fun getKeys(): List<String> = try {
        queries.getKeys().executeAsList()
    } catch (e: Exception) {
        emptyList()
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

    override fun observeBytes(key: String): Flow<ByteArray> = observe(key) { it }

    override fun getBoolean(key: String): Boolean? = get(key) { it[0] == 0x01.toByte() }

    override fun observeBoolean(key: String): Flow<Boolean> = observe(key) { it[0] == 0x01.toByte() }

    override fun getString(key: String): String? = get(key) { Buffer().write(it).readUtf8() }

    override fun observeString(key: String): Flow<String> = observe(key) { Buffer().write(it).readUtf8() }

    override fun getInt(key: String): Int? = get(key) { Buffer().write(it).readInt() }

    override fun observeInt(key: String): Flow<Int> = observe(key) { Buffer().write(it).readInt() }

    override fun getLong(key: String): Long? = get(key) { Buffer().write(it).readLong() }

    override fun observeLong(key: String): Flow<Long> = observe(key) { Buffer().write(it).readLong() }

    override fun getFloat(key: String): Float? = get(key) { Float.fromBits(Buffer().write(it).readInt()) }

    override fun observeFloat(key: String): Flow<Float> = observe(key) { Float.fromBits(Buffer().write(it).readInt()) }

    override fun getDouble(key: String): Double? = get(key) { Double.fromBits(Buffer().write(it).readLong()) }

    override fun observeDouble(key: String): Flow<Double> =
        observe(key) { Double.fromBits(Buffer().write(it).readLong()) }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? =
        get(key) { Cbor.decodeFromByteArray(deserializer, queries.get(key).executeAsOne()) }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T> =
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
            queries.put(key, mapper())
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private inline fun <T> observe(key: String, crossinline mapper: (bytes: ByteArray) -> T): Flow<T> = channelFlow {
        try {
            val listener = Query.Listener {
                val newValueBytes = get(key) { it } ?: return@Listener
                trySend(mapper(newValueBytes))
            }
            queries.get(key).addListener(listener)
            awaitClose { queries.get(key).removeListener(listener) }
        } catch (e: Exception) {
            // NoOp
        }
    }.buffer(1, BufferOverflow.DROP_OLDEST)
}
