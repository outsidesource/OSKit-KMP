package com.outsidesource.oskitkmp.filesystem

import android.annotation.SuppressLint
import androidx.core.net.toUri
import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.OkIoKmpIoSink
import com.outsidesource.oskitkmp.io.OkIoKmpIoSource
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.sink
import okio.source

@SuppressLint("Recycle")
actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.ReadWriteOnDirectory)

    return try {
        when (fsType) {
            KmpFsType.Internal -> Outcome.Ok(OkIoKmpIoSource(FileSystem.SYSTEM.source(ref.toPath())))
            KmpFsType.External -> {
                val context = AndroidExternalKmpFs.context ?: return Outcome.Error(KmpFsError.NotInitialized)
                val stream = context.applicationContext.contentResolver.openInputStream(ref.toUri())
                    ?: return Outcome.Error(KmpFsError.InvalidRef)
                val source = stream.source()
                Outcome.Ok(OkIoKmpIoSource(source))
            }
        }
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

@SuppressLint("Recycle")
actual suspend fun KmpFsRef.sink(mode: KmpFsWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.ReadWriteOnDirectory)

    return try {
        when (fsType) {
            KmpFsType.Internal -> Outcome.Ok(OkIoKmpIoSink(FileSystem.SYSTEM.sink(ref.toPath())))
            KmpFsType.External -> {
                val context = AndroidExternalKmpFs.context ?: return Outcome.Error(KmpFsError.NotInitialized)
                val modeString = when (mode) {
                    KmpFsWriteMode.Overwrite -> "wt"
                    KmpFsWriteMode.Append -> "wa"
                }

                val outputStream = context.applicationContext.contentResolver.openOutputStream(ref.toUri(), modeString)
                    ?: return Outcome.Error(KmpFsError.InvalidRef)
                val sink = outputStream.sink()
                Outcome.Ok(OkIoKmpIoSink(sink))
            }
        }
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}
