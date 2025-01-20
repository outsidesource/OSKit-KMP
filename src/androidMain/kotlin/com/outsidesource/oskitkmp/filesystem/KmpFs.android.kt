package com.outsidesource.oskitkmp.filesystem

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.OkIoKmpIoSink
import com.outsidesource.oskitkmp.io.OkIoKmpIoSource
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okio.sink
import okio.source

actual data class KmpFsContext(
    val applicationContext: Context,
    val activity: ComponentActivity,
)

@SuppressLint("Recycle")
actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        val context = AndroidExternalKmpFs.context ?: return Outcome.Error(KmpFsError.NotInitializedError)
        val stream = context.applicationContext.contentResolver.openInputStream(ref.toUri())
            ?: return Outcome.Error(KmpFsError.FileOpenError)
        val source = stream.source()
        Outcome.Ok(OkIoKmpIoSource(source))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}

@SuppressLint("Recycle")
actual suspend fun KmpFsRef.sink(mode: KmpFileWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        val context = AndroidExternalKmpFs.context ?: return Outcome.Error(KmpFsError.NotInitializedError)
        val modeString = when (mode) {
            KmpFileWriteMode.Overwrite -> "wt"
            KmpFileWriteMode.Append -> "wa"
        }

        val outputStream = context.applicationContext.contentResolver.openOutputStream(ref.toUri(), modeString)
            ?: return Outcome.Error(KmpFsError.FileCreateError)
        val sink = outputStream.sink()
        Outcome.Ok(OkIoKmpIoSink(sink))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}
