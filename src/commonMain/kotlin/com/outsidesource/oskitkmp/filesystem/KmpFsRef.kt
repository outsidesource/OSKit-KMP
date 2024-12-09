package com.outsidesource.oskitkmp.filesystem

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
        fun fromPersistableString(value: String): KmpFsRef {
            return cbor.decodeFromByteArray<KmpFsRef>(value.decodeBase64Bytes())
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun fromPersistableData(value: ByteArray): KmpFsRef {
            return cbor.decodeFromByteArray<KmpFsRef>(value)
        }
    }
}

expect suspend fun KmpFsRef.source(): Outcome<IKmpFsSource, Exception>
expect suspend fun KmpFsRef.sink(
    mode: KmpFileWriteMode = KmpFileWriteMode.Overwrite,
): Outcome<IKmpFsSink, Exception>

enum class KmpFileWriteMode {
    Append,
    Overwrite,
}
