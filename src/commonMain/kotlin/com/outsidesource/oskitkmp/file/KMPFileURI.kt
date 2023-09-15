package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Sink
import okio.Source

data class KMPFileURI internal constructor(
    internal val uri: String,
    val name: String,
    val isDirectory: Boolean,
) {
    fun toPersistableString(): String {
        TODO()
    }

    companion object {
        fun fromPersistableString(): KMPFileURI {
            TODO()
        }
    }
}

expect fun KMPFileURI.source(): Outcome<Source, Exception>
expect fun KMPFileURI.sink(mode: KMPFileWriteMode = KMPFileWriteMode.Overwrite): Outcome<Sink, Exception>

enum class KMPFileWriteMode {
    Append,
    Overwrite,
}
