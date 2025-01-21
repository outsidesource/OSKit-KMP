package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.WasmKmpIoSink
import com.outsidesource.oskitkmp.io.WasmKmpIoSource
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import org.w3c.files.File

actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
    when (type) {
        KmpFsType.Internal -> if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupportedError)
        KmpFsType.External -> if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupportedError)
    }

    val file = getFile().unwrapOrReturn { return it }
    return Outcome.Ok(WasmKmpIoSource(file))
}

actual suspend fun KmpFsRef.sink(mode: KmpFsWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
    when (type) {
        KmpFsType.Internal -> if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupportedError)
        KmpFsType.External -> if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupportedError)
    }

    val handle = WasmFsHandleRegister.getHandle(ref, WasmFsHandleAccessMode.Write)
        as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.NotFoundError)
    val writable = handle.createWritable(createWritableOptions(mode == KmpFsWriteMode.Append)).kmpAwaitOutcome()
        .unwrapOrReturn { return Outcome.Error(KmpFsError.OpenError) }

    if (mode == KmpFsWriteMode.Append) {
        val file = handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.OpenError) }
        writable.seek(file.size).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.OpenError) }
    }

    return Outcome.Ok(WasmKmpIoSink(writable))
}

private suspend fun KmpFsRef.getFile(): Outcome<File, KmpFsError> {
    val file = if (supportsFileSystemApi) {
        val handle = WasmFsHandleRegister.getHandle(ref)
            as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.NotFoundError)
        handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.OpenError) }
    } else {
        WasmFsHandleRegister.getHandle(ref) as? File ?: return Outcome.Error(KmpFsError.NotFoundError)
    }

    return Outcome.Ok(file)
}

internal actual suspend fun onKmpFileRefPersisted(ref: KmpFsRef) {
    val handle = WasmFsHandleRegister.getHandle(ref.ref) ?: return
    if (handle !is FileSystemHandle) return
    WasmFsHandleRegister.persistHandle(ref.ref, handle)
}

internal actual suspend fun internalClearPersistedDataCache(ref: KmpFsRef?) {
    WasmFsHandleRegister.removePersistedHandle(ref?.ref)
}
