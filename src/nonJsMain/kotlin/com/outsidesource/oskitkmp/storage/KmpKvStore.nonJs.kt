package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.storage.sqldelight.KmpKvStoreDatabaseQueries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import okio.Buffer

internal expect class KmpKvStoreContext

internal expect fun createDatabaseDriver(context: KmpKvStoreContext, nodeName: String): SqlDriver

class KmpKvStoreNode internal constructor(
    context: KmpKvStoreContext,
    private val name: String,
) : IKmpKvStoreNode {
    private val driver = createDatabaseDriver(context, name)
    private val queries = KmpKvStoreDatabaseQueries(driver)

    override suspend fun close() = driver.close()

    override suspend fun contains(key: String): Boolean = try {
        queries.exists(key).executeAsOneOrNull() != null
    } catch (e: Exception) {
        false
    }

    override suspend fun remove(key: String): Outcome<Unit, Exception> = try {
        queries.remove(key)
        KmpKvStoreObserverRegistry.notifyValueChange(name, key, null)
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override suspend fun clear(): Outcome<Unit, Exception> = try {
        queries.clear()
        KmpKvStoreObserverRegistry.notifyClear(name)
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override suspend fun vacuum(): Outcome<Unit, Exception> = try {
        queries.vacuum()
        Outcome.Ok(Unit)
    } catch (e: Exception) {
        Outcome.Error(e)
    }

    override suspend fun keys(): Set<String> = try {
        queries.getKeys().executeAsList().toSet()
    } catch (e: Exception) {
        emptySet()
    }

    override suspend fun keyCount(): Long = try {
        queries.getKeyCount().executeAsOne()
    } catch (e: Exception) {
        0
    }

    override suspend fun dbFileSize(): Long = try {
        driver.executeQuery(
            identifier = null,
            sql = "SELECT `page_count` * `page_size` as `size` FROM pragma_page_count(), pragma_page_size();",
            mapper = { QueryResult.Value(it.getLong(0)) },
            parameters = 0,
        ).value ?: 0
    } catch (e: Exception) {
        0
    }

    override suspend fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> = put(key) { value }
    override suspend fun getBytes(key: String): ByteArray? = get(key) { it }
    override suspend fun observeBytes(key: String): Flow<ByteArray?> = observe(key) { it }

    override suspend fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> =
        put(key) { byteArrayOf(if (value) 0x01 else 0x00) }
    override suspend fun getBoolean(key: String): Boolean? = get(key) { it[0] == 0x01.toByte() }
    override suspend fun observeBoolean(key: String): Flow<Boolean?> = observe(key) { it[0] == 0x01.toByte() }

    override suspend fun putString(key: String, value: String): Outcome<Unit, Exception> =
        put(key) { value.encodeToByteArray() }
    override suspend fun getString(key: String): String? = get(key) { Buffer().write(it).readUtf8() }
    override suspend fun observeString(key: String): Flow<String?> = observe(key) { Buffer().write(it).readUtf8() }

    override suspend fun putInt(key: String, value: Int): Outcome<Unit, Exception> =
        put(key) { Buffer().writeInt(value).readByteArray() }
    override suspend fun getInt(key: String): Int? = get(key) { Buffer().write(it).readInt() }
    override suspend fun observeInt(key: String): Flow<Int?> = observe(key) { Buffer().write(it).readInt() }

    override suspend fun putLong(key: String, value: Long): Outcome<Unit, Exception> =
        put(key) { Buffer().writeLong(value).readByteArray() }
    override suspend fun getLong(key: String): Long? = get(key) { Buffer().write(it).readLong() }
    override suspend fun observeLong(key: String): Flow<Long?> = observe(key) { Buffer().write(it).readLong() }

    override suspend fun putFloat(key: String, value: Float): Outcome<Unit, Exception> =
        put(key) { Buffer().writeInt(value.toBits()).readByteArray() }
    override suspend fun getFloat(key: String): Float? = get(key) { Float.fromBits(Buffer().write(it).readInt()) }
    override suspend fun observeFloat(key: String): Flow<Float?> =
        observe(key) { Float.fromBits(Buffer().write(it).readInt()) }

    override suspend fun putDouble(key: String, value: Double): Outcome<Unit, Exception> =
        put(key) { Buffer().writeLong(value.toBits()).readByteArray() }
    override suspend fun getDouble(key: String): Double? = get(key) { Double.fromBits(Buffer().write(it).readLong()) }
    override suspend fun observeDouble(key: String): Flow<Double?> =
        observe(key) { Double.fromBits(Buffer().write(it).readLong()) }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> = put(key) { Cbor.encodeToByteArray(serializer, value) }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? =
        get(key) { Cbor.decodeFromByteArray(deserializer, queries.get(key).executeAsOne()) }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T?> =
        observe(key) { Cbor.decodeFromByteArray(deserializer, it) }

    override suspend fun transaction(block: suspend (rollback: () -> Nothing) -> Unit) {
        queries.transaction {
            runBlocking {
                block(::rollback)
            }
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
            KmpKvStoreObserverRegistry.notifyValueChange(name, key, value)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private inline fun <T> observe(key: String, crossinline mapper: (rawValue: ByteArray) -> T?): Flow<T?> {
        return KmpKvStoreObserverRegistry.observe<ByteArray, T>(nodeName = name, key = key, mapper)
            .onStart { emit(get(key, mapper)) }
    }
}
