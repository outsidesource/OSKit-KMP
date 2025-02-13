package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.outcome.Outcome
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
private val cbor = Cbor {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Represents a lightweight reference to a file/directory location. Refs do not contain
 * any file data or file handles themselves, only a reference to a location. Refs also contain basic information like
 * file/directory name and if it is a file or directory.
 */
@Serializable
data class KmpFsRef internal constructor(
    @SerialName("1")
    internal val ref: String,
    @SerialName("2")
    val name: String,
    @SerialName("3")
    val isDirectory: Boolean,
    @SerialName("4")
    val fsType: KmpFsType,
) {

    /**
     * Creates a string that is safe for persisting to a key/value store or a database for future use with [KmpFsRef.fromPersistableString]
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun toPersistableString(): String {
        onKmpFileRefPersisted(this)
        return cbor.encodeToByteArray(this).encodeBase64()
    }

    /**
     * Creates a byteArray that is safe for persisting to a key/value store or a database for future use with  [KmpFsRef.fromPersistableBytes]
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun toPersistableBytes(): ByteArray {
        onKmpFileRefPersisted(this)
        return cbor.encodeToByteArray(this)
    }

    /**
     * Clears any cached data for a given ref. The WASM target requires caching ref data in a specific way to persist
     * references. This will clear any cache data and render the persisted ref useless after the current session.
     */
    suspend fun clearPersistedDataCache() = internalClearPersistedDataCache(this)

    companion object {
        /**
         * Restores a [KmpFsRef] from a string created by [KmpFsRef.fromPersistableString]
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableString(value: String): KmpFsRef {
            return cbor.decodeFromByteArray<KmpFsRef>(value.decodeBase64Bytes())
        }

        /**
         * Restores a [KmpFsRef] from a string created by [KmpFsRef.fromPersistableBytes]
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableBytes(value: ByteArray): KmpFsRef {
            return cbor.decodeFromByteArray<KmpFsRef>(value)
        }

        /**
         * Clears any cached data for all refs. The WASM target requires caching ref data in a specific way to persist
         * references. This will clear any cache data and render all persisted refs useless after the current session.
         */
        suspend fun clearPersistedDataCache() = internalClearPersistedDataCache(null)
    }
}

/**
 * The [KmpFs] API being used
 */
enum class KmpFsType {
    Internal,
    External,
}

/**
 * Creates a readable source for the ref
 */
expect suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError>

/**
 * Creates a writable sink for the ref
 */
expect suspend fun KmpFsRef.sink(mode: KmpFsWriteMode = KmpFsWriteMode.Overwrite): Outcome<IKmpIoSink, KmpFsError>

/**
 * The write mode for a sink
 */
enum class KmpFsWriteMode {
    /**
     * Appends to the existing content
     */
    Append,

    /**
     * Overwrites any existing content
     */
    Overwrite,
}

internal expect suspend fun onKmpFileRefPersisted(ref: KmpFsRef)
internal expect suspend fun internalClearPersistedDataCache(ref: KmpFsRef?)
