package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

internal class IndexedDbWasmKmpKVStoreNode(val nodeName: String) : IKmpKVStoreNode {
    override suspend fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun contains(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun remove(key: String): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun clear(): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun vacuum(): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun keys(): Set<String> {
        TODO("Not yet implemented")
    }

    override suspend fun keyCount(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun dbFileSize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun putBytes(
        key: String,
        value: ByteArray,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getBytes(key: String): ByteArray? {
        TODO("Not yet implemented")
    }

    override suspend fun observeBytes(key: String): Flow<ByteArray?> {
        TODO("Not yet implemented")
    }

    override suspend fun putBoolean(
        key: String,
        value: Boolean,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getBoolean(key: String): Boolean? {
        TODO("Not yet implemented")
    }

    override suspend fun observeBoolean(key: String): Flow<Boolean?> {
        TODO("Not yet implemented")
    }

    override suspend fun putString(
        key: String,
        value: String,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getString(key: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun observeString(key: String): Flow<String?> {
        TODO("Not yet implemented")
    }

    override suspend fun putInt(
        key: String,
        value: Int,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getInt(key: String): Int? {
        TODO("Not yet implemented")
    }

    override suspend fun observeInt(key: String): Flow<Int?> {
        TODO("Not yet implemented")
    }

    override suspend fun putLong(
        key: String,
        value: Long,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getLong(key: String): Long? {
        TODO("Not yet implemented")
    }

    override suspend fun observeLong(key: String): Flow<Long?> {
        TODO("Not yet implemented")
    }

    override suspend fun putFloat(
        key: String,
        value: Float,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getFloat(key: String): Float? {
        TODO("Not yet implemented")
    }

    override suspend fun observeFloat(key: String): Flow<Float?> {
        TODO("Not yet implemented")
    }

    override suspend fun putDouble(
        key: String,
        value: Double,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun getDouble(key: String): Double? {
        TODO("Not yet implemented")
    }

    override suspend fun observeDouble(key: String): Flow<Double?> {
        TODO("Not yet implemented")
    }

    override suspend fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun <T> getSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): T? {
        TODO("Not yet implemented")
    }

    override suspend fun <T> observeSerializable(
        key: String,
        deserializer: DeserializationStrategy<T>,
    ): Flow<T?> {
        TODO("Not yet implemented")
    }

    override suspend fun transaction(block: suspend (() -> Nothing) -> Unit) {
        TODO("Not yet implemented")
    }
}
