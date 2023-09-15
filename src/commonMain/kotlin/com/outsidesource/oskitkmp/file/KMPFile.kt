package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Sink
import okio.Source

data class KMPFile internal constructor(
    internal val path: String,
    val name: String,
    val isDirectory: Boolean,
)

expect fun KMPFile.source(): Outcome<Source, Exception>
expect fun KMPFile.sink(mode: KMPFileWriteMode = KMPFileWriteMode.Overwrite): Outcome<Sink, Exception>

enum class KMPFileWriteMode {
    Append,
    Overwrite,
}
