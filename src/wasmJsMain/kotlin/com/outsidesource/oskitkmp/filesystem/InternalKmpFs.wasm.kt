package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.coroutines.*
import org.w3c.files.File

actual fun platformInternalKmpFs(): IInternalKmpFs = WasmInternalKmpFs()

internal class WasmInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {
    private var context: KmpFsContext? = null
    private var internalRoot = CompletableDeferred<KmpFsRef>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val root: KmpFsRef = KmpFsRef("00000000000000000000000000", "", true, KmpFsType.Internal)

    override fun init(context: KmpFsContext) {
        this.context = context
        scope.launch {
            val rootHandle = navigator.storage.getDirectory().kmpAwaitOutcome().unwrapOrReturn {
                internalRoot.completeExceptionally(KmpFsError.NotInitialized)
                return@launch
            }
            val key = WasmFsHandleRegister.putHandle(rootHandle)
            val ref = KmpFsRef(ref = key, name = rootHandle.name, isDirectory = true, type = KmpFsType.Internal)
            internalRoot.complete(ref)
        }
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupported)
        val parentHandle = WasmFsHandleRegister.getHandle(awaitRef(dir).ref)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        val handle = parentHandle.getFileHandle(fileName, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(if (create) KmpFsError.RefNotCreated else KmpFsError.RefNotFound) }
        if (handle.kind == FileSystemHandleKind.Directory.value) return Outcome.Error(KmpFsError.RefExistsAsDirectory)

        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = false, type = KmpFsType.Internal))
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupported)
        val parentHandle = WasmFsHandleRegister.getHandle(awaitRef(dir).ref, WasmFsHandleAccessMode.Write)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        val handle = parentHandle.getDirectoryHandle(name, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(if (create) KmpFsError.RefNotCreated else KmpFsError.RefNotFound) }
        if (handle.kind == FileSystemHandleKind.File.value) return Outcome.Error(KmpFsError.RefExistsAsFile)

        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = true, type = KmpFsType.Internal))
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupported)
        val handle = WasmFsHandleRegister.getHandle(
            key = awaitRef(ref).ref,
            mode = WasmFsHandleAccessMode.Write,
        ) as? FileSystemHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        handle
            .remove(removeOptions(true))
            .kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
        return Outcome.Ok(Unit)
    }

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupported)
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val handle = WasmFsHandleRegister
                .getHandle(awaitRef(dir).ref, WasmFsHandleAccessMode.Read) as? FileSystemDirectoryHandle
                ?: return Outcome.Error(KmpFsError.InvalidRef)

            if (!isRecursive) {
                val list = handle.entries().map {
                    val key = WasmFsHandleRegister.putHandle(it)
                    KmpFsRef(
                        ref = key,
                        name = it.name,
                        isDirectory = it is FileSystemDirectoryHandle,
                        type = KmpFsType.Internal,
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
                            type = KmpFsType.Internal,
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
        if (supportsOpfs) {
            val handle = WasmFsHandleRegister.getHandle(awaitRef(ref).ref)
                as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
            val file = handle
                .getFile()
                .kmpAwaitOutcome()
                .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
            return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
        }

        val file = WasmFsHandleRegister.getHandle(awaitRef(ref).ref) as? File
            ?: return Outcome.Error(KmpFsError.InvalidRef)
        return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        if (!supportsOpfs) return false
        val handle = WasmFsHandleRegister.getHandle(awaitRef(ref).ref) as? FileSystemHandle ?: return false
        if (handle is FileSystemFileHandle) handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return false }
        return true
    }

    private suspend inline fun awaitRef(ref: KmpFsRef): KmpFsRef = if (ref == root) internalRoot.await() else ref
}
