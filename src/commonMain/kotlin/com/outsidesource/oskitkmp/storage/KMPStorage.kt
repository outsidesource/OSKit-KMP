package com.outsidesource.oskitkmp.storage

import app.cash.sqldelight.db.SqlDriver
import okio.Buffer

expect class KMPStorageContext : IKMPStorageContext

interface IKMPStorageContext {
    val appName: String
}

interface IKMPStorage {
    fun close()
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()
    fun putBytes(key: String, value: ByteArray)
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putFloat(key: String, value: Float)
    fun putDouble(key: String, value: Double)
    fun getBytes(key: String): ByteArray
    fun getString(key: String): String
    fun getInt(key: String): Int
    fun getLong(key: String): Long
    fun getFloat(key: String): Float
    fun getDouble(key: String): Double
    fun transaction(block: (rollback: () -> Nothing) -> Unit)
}

// TODO: Try/Catches and getOrNull
// TODO: Custom Data Store Name
// TODO: Listener
// TODO: Last updated?
// TODO: Encryption? Could be part of context

class KMPStorage(context: KMPStorageContext) : IKMPStorage {
    private val driver = createDatabaseDriver(context)
    private val queries = KMPStorageDatabaseQueries(driver)

    override fun close() {
        driver.close()
    }

    override fun contains(key: String): Boolean {
        return queries.exists(key).executeAsOneOrNull() != null
    }

    override fun remove(key: String) {
        queries.remove(key)
    }

    override fun clear() {
        queries.clear()
    }

    override fun putBytes(key: String, value: ByteArray) {
        queries.put(key, value)
    }

    override fun putString(key: String, value: String) {
        queries.put(key, value.encodeToByteArray())
    }

    override fun putInt(key: String, value: Int) {
        queries.put(key, Buffer().writeInt(value).readByteArray())
    }

    override fun putLong(key: String, value: Long) {
        queries.put(key, Buffer().writeLong(value).readByteArray())
    }

    override fun putFloat(key: String, value: Float) {
        queries.put(key, Buffer().writeInt(value.toBits()).readByteArray())
    }

    override fun putDouble(key: String, value: Double) {
        queries.put(key, Buffer().writeLong(value.toBits()).readByteArray())
    }

    override fun getBytes(key: String): ByteArray {
        return queries.get(key).executeAsOne().value_
    }

    override fun getString(key: String): String {
        return Buffer().write(queries.get(key).executeAsOne().value_).readUtf8()
    }

    override fun getInt(key: String): Int {
        return Buffer().write(queries.get(key).executeAsOne().value_).readInt()
    }

    override fun getLong(key: String): Long {
        return Buffer().write(queries.get(key).executeAsOne().value_).readLong()
    }

    override fun getFloat(key: String): Float {
        return Float.fromBits(Buffer().write(queries.get(key).executeAsOne().value_).readInt())
    }

    override fun getDouble(key: String): Double {
        return Double.fromBits(Buffer().write(queries.get(key).executeAsOne().value_).readLong())
    }

    override fun transaction(block: (rollback: () -> Nothing) -> Unit) {
        queries.transaction {
            block(::rollback)
        }
    }
}

internal expect fun createDatabaseDriver(context: KMPStorageContext): SqlDriver
