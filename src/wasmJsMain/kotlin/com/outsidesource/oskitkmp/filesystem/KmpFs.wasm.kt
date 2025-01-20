package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.WasmKmpIoSink
import com.outsidesource.oskitkmp.io.WasmKmpIoSource
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import org.w3c.files.File

actual class KmpFsContext()

actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
    val file = getFile().unwrapOrReturn { return it }
    return Outcome.Ok(WasmKmpIoSource(file))
}

actual suspend fun KmpFsRef.sink(mode: KmpFileWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
    if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupportedError)

    val handle = WasmFileHandleRegister.getHandle(ref, WasmFileHandleAccessMode.Write)
        as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.FileNotFoundError)
    val writable = handle.createWritable(createWritableOptions(mode == KmpFileWriteMode.Append)).kmpAwaitOutcome()
        .unwrapOrReturn { return Outcome.Error(KmpFsError.FileOpenError) }

    if (mode == KmpFileWriteMode.Append) {
        val file = handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.FileOpenError) }
        writable.seek(file.size).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.FileOpenError) }
    }

    return Outcome.Ok(WasmKmpIoSink(writable))
}

private suspend fun KmpFsRef.getFile(): Outcome<File, KmpFsError> {
    val file = if (supportsFileSystemApi) {
        val handle = WasmFileHandleRegister.getHandle(ref)
            as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.FileNotFoundError)
        handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.FileOpenError) }
    } else {
        WasmFileHandleRegister.getHandle(ref) as? File ?: return Outcome.Error(KmpFsError.FileNotFoundError)
    }

    return Outcome.Ok(file)
}
