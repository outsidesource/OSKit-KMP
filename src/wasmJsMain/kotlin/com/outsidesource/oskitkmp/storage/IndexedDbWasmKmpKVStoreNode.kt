package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

internal class IndexedDbWasmKmpKVStoreNode(val nodeName: String) : IKmpKVStoreNode {

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun contains(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(key: String): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override fun clear(): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override fun vacuum(): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override fun getKeys(): Set<String> {
        TODO("Not yet implemented")
    }

    override fun keyCount(): Long {
        TODO("Not yet implemented")
    }

    override fun dbFileSize(): Long {
        TODO("Not yet implemented")
    }

    override fun putBytes(key: String, value: ByteArray): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getBytes(key: String): ByteArray? {
        TODO("Not yet implemented")
    }
    override fun observeBytes(key: String): Flow<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun putBoolean(key: String, value: Boolean): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getBoolean(key: String): Boolean? {
        TODO("Not yet implemented")
    }
    override fun observeBoolean(key: String): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun putString(key: String, value: String): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getString(key: String): String? {
        TODO("Not yet implemented")
    }
    override fun observeString(key: String): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun putInt(key: String, value: Int): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getInt(key: String): Int? {
        TODO("Not yet implemented")
    }
    override fun observeInt(key: String): Flow<Int> {
        TODO("Not yet implemented")
    }

    override fun putLong(key: String, value: Long): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getLong(key: String): Long? {
        TODO("Not yet implemented")
    }
    override fun observeLong(key: String): Flow<Long> {
        TODO("Not yet implemented")
    }

    override fun putFloat(key: String, value: Float): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getFloat(key: String): Float? {
        TODO("Not yet implemented")
    }
    override fun observeFloat(key: String): Flow<Float> {
        TODO("Not yet implemented")
    }

    override fun putDouble(key: String, value: Double): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun getDouble(key: String): Double? {
        TODO("Not yet implemented")
    }
    override fun observeDouble(key: String): Flow<Double> {
        TODO("Not yet implemented")
    }

    override fun <T> putSerializable(
        key: String,
        value: T,
        serializer: SerializationStrategy<T>,
    ): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }
    override fun <T> getSerializable(key: String, deserializer: DeserializationStrategy<T>): T? {
        TODO("Not yet implemented")
    }
    override fun <T> observeSerializable(key: String, deserializer: DeserializationStrategy<T>): Flow<T> {
        TODO("Not yet implemented")
    }

    override fun transaction(block: (() -> Nothing) -> Unit) {
        TODO("Not yet implemented")
    }
}
