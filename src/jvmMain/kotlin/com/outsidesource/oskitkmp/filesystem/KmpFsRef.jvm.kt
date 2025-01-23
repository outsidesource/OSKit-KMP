package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.OkIoKmpIoSink
import com.outsidesource.oskitkmp.io.OkIoKmpIoSource
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path.Companion.toPath

actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.ReadWriteToDirectory)
        val source = FileSystem.SYSTEM.source(ref.toPath())
        Outcome.Ok(OkIoKmpIoSource(source))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

actual suspend fun KmpFsRef.sink(mode: KmpFsWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.ReadWriteToDirectory)
        val sink = if (mode == KmpFsWriteMode.Append) {
            FileSystem.SYSTEM.appendingSink(ref.toPath())
        } else {
            FileSystem.SYSTEM.sink(ref.toPath())
        }
        Outcome.Ok(OkIoKmpIoSink(sink))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}
