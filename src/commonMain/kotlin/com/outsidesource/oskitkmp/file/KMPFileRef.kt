package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Sink
import okio.Source

data class KMPFileRef internal constructor(
    internal val ref: String,
    val name: String,
    val isDirectory: Boolean,
) {
    fun toPersistableString(): String {
        TODO()
    }

    companion object {
        fun fromPersistableString(): KMPFileRef {
            TODO()
        }
    }
}

expect fun KMPFileRef.source(): Outcome<Source, Exception>
expect fun KMPFileRef.sink(mode: KMPFileWriteMode = KMPFileWriteMode.Overwrite): Outcome<Sink, Exception>

enum class KMPFileWriteMode {
    Append,
    Overwrite,
}
