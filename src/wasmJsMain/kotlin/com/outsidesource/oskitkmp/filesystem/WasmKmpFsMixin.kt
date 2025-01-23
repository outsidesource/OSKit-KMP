package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import org.w3c.files.File

internal class WasmKmpFsMixin(
    private val fsType: KmpFsType,
    private val sanitizeRef: suspend (KmpFsRef) -> KmpFsRef,
    private val isInitialized: () -> Boolean,
) : IKmpFs {

    val isPlatformApiSupported = when (fsType) {
        KmpFsType.Internal -> supportsOpfs
        KmpFsType.External -> supportsFileSystemApi
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
        if (!isPlatformApiSupported) return Outcome.Error(KmpFsError.NotSupported)
        if (dir.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
        if (!dir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)

        val parentHandle = WasmFsHandleRegister.getHandle(sanitizeRef(dir).ref)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        val handle = parentHandle.getFileHandle(fileName, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(if (create) KmpFsError.RefNotCreated else KmpFsError.RefNotFound) }
        if (handle.kind == FileSystemHandleKind.Directory.value) return Outcome.Error(KmpFsError.RefExistsAsDirectory)

        val key = WasmFsHandleRegister.putHandle(handle)
        val ref = KmpFsRef(
            ref = key,
            name = handle.name,
            isDirectory = false,
            fsType = fsType,
        )
        return Outcome.Ok(ref)
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
        if (!isPlatformApiSupported) return Outcome.Error(KmpFsError.NotSupported)
        if (dir.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
        if (!dir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)

        val parentHandle = WasmFsHandleRegister.getHandle(sanitizeRef(dir).ref, WasmFsHandleAccessMode.Write)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        val handle = parentHandle.getDirectoryHandle(name, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(if (create) KmpFsError.RefNotCreated else KmpFsError.RefNotFound) }
        if (handle.kind == FileSystemHandleKind.File.value) return Outcome.Error(KmpFsError.RefExistsAsFile)

        val key = WasmFsHandleRegister.putHandle(handle)
        val ref = KmpFsRef(
            ref = key,
            name = handle.name,
            isDirectory = true,
            fsType = fsType,
        )

        return Outcome.Ok(ref)
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
        if (!isPlatformApiSupported) return Outcome.Error(KmpFsError.NotSupported)
        if (ref.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)

        val handle = WasmFsHandleRegister.getHandle(
            key = sanitizeRef(ref).ref,
            mode = WasmFsHandleAccessMode.Write,
        ) as? FileSystemHandle ?: return Outcome.Error(KmpFsError.InvalidRef)

        handle
            .remove(removeOptions(true))
            .kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }

        return Outcome.Ok(Unit)
    }

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> {
        if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
        if (!isPlatformApiSupported) return Outcome.Error(KmpFsError.NotSupported)
        if (dir.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
        if (!dir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)

        return try {
            val handle = WasmFsHandleRegister
                .getHandle(sanitizeRef(dir).ref, WasmFsHandleAccessMode.Read) as? FileSystemDirectoryHandle
                ?: return Outcome.Error(KmpFsError.InvalidRef)

            if (!isRecursive) {
                val list = handle.entries().map {
                    val key = WasmFsHandleRegister.putHandle(it)
                    KmpFsRef(
                        ref = key,
                        name = it.name,
                        isDirectory = it is FileSystemDirectoryHandle,
                        fsType = fsType,
                    )
                }
                return Outcome.Ok(list)
            }

            val list = handle.entries().flatMap {
                buildList {
                    val key = WasmFsHandleRegister.putHandle(it)
                    val childHandle =
                        KmpFsRef(
                            ref = key,
                            name = it.name,
                            isDirectory = it is FileSystemDirectoryHandle,
                            fsType = fsType,
                        )

                    add(childHandle)

                    if (it is FileSystemDirectoryHandle) {
                        addAll(list(childHandle).unwrapOrNull() ?: emptyList())
                    }
                }
            }

            return Outcome.Ok(list)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> {
        if (!isInitialized()) return Outcome.Error(KmpFsError.NotInitialized)
        if (ref.fsType != fsType) return Outcome.Error(KmpFsError.RefFsType)
        if (isPlatformApiSupported) {
            val handle = WasmFsHandleRegister.getHandle(sanitizeRef(ref).ref)
                as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
            val file = handle
                .getFile()
                .kmpAwaitOutcome()
                .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
            return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
        }

        // TODO: Fix this line
        if (fsType != KmpFsType.External) return Outcome.Error(KmpFsError.NotSupported)

        val file = WasmFsHandleRegister.getHandle(sanitizeRef(ref).ref) as? File
            ?: return Outcome.Error(KmpFsError.InvalidRef)

        return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        if (!isInitialized()) return false
        if (!isPlatformApiSupported) return false
        if (ref.fsType != fsType) return false

        val handle = WasmFsHandleRegister.getHandle(sanitizeRef(ref).ref) as? FileSystemHandle ?: return false
        if (handle is FileSystemFileHandle) handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return false }
        return true
    }
}
