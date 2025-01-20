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

@Serializable
data class KmpFsRef internal constructor(
    @SerialName("1")
    internal val ref: String,
    @SerialName("2")
    val name: String,
    @SerialName("3")
    val isDirectory: Boolean,
    @SerialName("4")
    val type: KmpFsRefType,
) {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun toPersistableString(): String {
        onKmpFileRefPersisted(this)
        return cbor.encodeToByteArray(this).encodeBase64()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun toPersistableData(): ByteArray {
        onKmpFileRefPersisted(this)
        return cbor.encodeToByteArray(this)
    }

    /**
     * Clears any cached data for a given ref. The WASM target requires caching ref data in a specific way to persist
     * references. This will clear any cache data and render the persisted ref useless after the current session.
     */
    suspend fun clearPersistedDataCache() = internalClearPersistedDataCache(this)

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableString(value: String): KmpFsRef {
            return cbor.decodeFromByteArray<KmpFsRef>(value.decodeBase64Bytes())
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableData(value: ByteArray): KmpFsRef {
            return cbor.decodeFromByteArray<KmpFsRef>(value)
        }

        /**
         * Clears any cached data for all refs. The WASM target requires caching ref data in a specific way to persist
         * references. This will clear any cache data and render all persisted refs useless after the current session.
         */
        suspend fun clearPersistedDataCache() = internalClearPersistedDataCache(null)
    }
}

enum class KmpFsRefType {
    Internal,
    External,
}

internal expect suspend fun onKmpFileRefPersisted(ref: KmpFsRef)
internal expect suspend fun internalClearPersistedDataCache(ref: KmpFsRef?)

expect suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError>
expect suspend fun KmpFsRef.sink(mode: KmpFsWriteMode = KmpFsWriteMode.Overwrite): Outcome<IKmpIoSink, KmpFsError>

enum class KmpFsWriteMode {
    Append,
    Overwrite,
}
