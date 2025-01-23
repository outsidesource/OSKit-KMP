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
    if (isDirectory) return Outcome.Error(KmpFsError.ReadWriteToDirectory)
    when (fsType) {
        KmpFsType.Internal -> if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupported)
        KmpFsType.External -> if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)
    }

    val file = getFile().unwrapOrReturn { return it }
    return Outcome.Ok(WasmKmpIoSource(file))
}

actual suspend fun KmpFsRef.sink(mode: KmpFsWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    if (isDirectory) return Outcome.Error(KmpFsError.ReadWriteToDirectory)
    when (fsType) {
        KmpFsType.Internal -> if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupported)
        KmpFsType.External -> if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)
    }

    val handle = WasmFsHandleRegister.getHandle(ref, WasmFsHandleAccessMode.Write)
        as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
    val writable = handle.createWritable(createWritableOptions(mode == KmpFsWriteMode.Append)).kmpAwaitOutcome()
        .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }

    if (mode == KmpFsWriteMode.Append) {
        val file = handle.getFile().kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
        writable.seek(file.size).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
    }

    return Outcome.Ok(WasmKmpIoSink(writable))
}

private suspend fun KmpFsRef.getFile(): Outcome<File, KmpFsError> {
    val file = if (supportsFileSystemApi) {
        val handle = WasmFsHandleRegister.getHandle(ref)
            as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
    } else {
        WasmFsHandleRegister.getHandle(ref) as? File ?: return Outcome.Error(KmpFsError.InvalidRef)
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
