package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import io.ktor.util.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import okio.Sink
import okio.Source

@OptIn(ExperimentalSerializationApi::class)
private val cbor = Cbor {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class KMPFileRef internal constructor(
    @SerialName("1")
    internal val ref: String,
    @SerialName("2")
    val name: String,
    @SerialName("3")
    val isDirectory: Boolean,
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun toPersistableString(): String {
        return cbor.encodeToByteArray(this).encodeBase64()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun toPersistableData(): ByteArray {
        return cbor.encodeToByteArray(this)
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableString(value: String): KMPFileRef {
            return cbor.decodeFromByteArray<KMPFileRef>(value.decodeBase64Bytes())
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableData(value: ByteArray): KMPFileRef {
            return cbor.decodeFromByteArray<KMPFileRef>(value)
        }
    }
}

expect fun KMPFileRef.source(): Outcome<Source, Exception>
expect fun KMPFileRef.sink(mode: KMPFileWriteMode = KMPFileWriteMode.Overwrite): Outcome<Sink, Exception>

enum class KMPFileWriteMode {
    Append,
    Overwrite,
}
