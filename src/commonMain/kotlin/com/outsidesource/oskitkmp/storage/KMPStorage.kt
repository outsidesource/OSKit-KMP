package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

/**
 * [KMPStorage] is a multiplatform key value store that allows persistent storage of any data
 *
 * All [KMPStorage] and [KMPStorageNode] methods are blocking and should be run in a coroutine on
 * [Dispatchers.IO]
 *
 * To use [KMPStorage] create an instance of each platform independent implementation, [AndroidKMPStorage],
 * [IOSKMPStorage], [DesktopKMPStorage] (JVM). Each implementation implements [IKMPStorage].
 *
 * Desktop/JVM Note: You may need to add the `java.sql` module:
 * ```
 * compose.desktop {
 *    nativeDistributions {
 *        modules("java.sql")
 *    }
 * }
 * ```
 *
 * iOS Note: You may need to add the linker flag `-lsqlite3`
 */
interface IKMPStorage {
    fun openNode(nodeName: String): Outcome<IKMPStorageNode, Exception>
}

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
