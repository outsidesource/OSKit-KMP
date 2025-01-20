package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.OkIoKmpIoSink
import com.outsidesource.oskitkmp.io.OkIoKmpIoSource
import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.FileDialog
import java.awt.Frame

actual class KmpFsContext(val window: Frame, val appName: String)

actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        val source = FileSystem.SYSTEM.source(ref.toPath())
        Outcome.Ok(OkIoKmpIoSource(source))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

actual suspend fun KmpFsRef.sink(mode: KmpFileWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        val sink = if (mode == KmpFileWriteMode.Append) {
            FileSystem.SYSTEM.appendingSink(ref.toPath())
        } else {
            FileSystem.SYSTEM.sink(ref.toPath())
        }
        Outcome.Ok(OkIoKmpIoSink(sink))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}
