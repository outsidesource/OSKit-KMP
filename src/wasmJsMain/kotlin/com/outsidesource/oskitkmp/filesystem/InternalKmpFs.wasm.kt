package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.w3c.files.File

actual fun platformInternalKmpFs(): IInternalKmpFs = WasmInternalKmpFs()

internal class WasmInternalKmpFs() : IInternalKmpFs, IInitializableKmpFs {
    private var context: KmpFsContext? = null
    private var internalRoot = atomic<KmpFsRef?>(null)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val root: KmpFsRef by lazy {
        internalRoot.value ?: KmpFsRef("", "", true, KmpFsRefType.Internal) // TODO: CHANGE THIS
    }

    override fun init(context: KmpFsContext) {
        this.context = context
        scope.launch {
            val rootHandle = navigator.storage.getDirectory().kmpAwaitOutcome().unwrapOrReturn { return@launch }
            val key = WasmFsHandleRegister.putHandle(rootHandle)
            val ref = KmpFsRef(ref = key, name = rootHandle.name, isDirectory = false, type = KmpFsRefType.Internal)
            internalRoot.value = ref
        }
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupportedError)
        val parentHandle = WasmFsHandleRegister.getHandle(dir.ref)
                as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.FileOpenError)
        val handle = parentHandle.getFileHandle(fileName, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.FileOpenError) }
        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = false, type = KmpFsRefType.Internal))
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupportedError)
        val parentHandle = WasmFsHandleRegister.getHandle(dir.ref, WasmFsHandleAccessMode.Write)
                as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.FileOpenError)
        val handle = parentHandle.getDirectoryHandle(name, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.FileOpenError) }
        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = true, type = KmpFsRefType.Internal))
    }

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        return Outcome.Error(KmpFsError.NotSupportedError)
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupportedError)
        val handle = WasmFsHandleRegister.getHandle(ref.ref, WasmFsHandleAccessMode.Write) as? FileSystemHandle
            ?: return Outcome.Error(KmpFsError.FileOpenError)
        handle
            .remove(removeOptions(true))
            .kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.FileDeleteError) }
        return Outcome.Ok(Unit)
    }

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> {
        if (!supportsOpfs) return Outcome.Error(KmpFsError.NotSupportedError)
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val handle = WasmFsHandleRegister
                .getHandle(dir.ref, WasmFsHandleAccessMode.Read) as? FileSystemDirectoryHandle
                ?: return Outcome.Error(KmpFsError.DirectoryListError)

            if (!isRecursive) {
                val list = handle.entries().map {
                    val key = WasmFsHandleRegister.putHandle(it)
                    KmpFsRef(
                        ref = key,
                        name = it.name,
                        isDirectory = it is FileSystemDirectoryHandle,
                        type = KmpFsRefType.Internal,
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
                            type = KmpFsRefType.Internal,
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
            val handle = WasmFsHandleRegister.getHandle(ref.ref)
                    as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.FileNotFoundError)
            val file = handle
                .getFile()
                .kmpAwaitOutcome()
                .unwrapOrReturn { return Outcome.Error(KmpFsError.FileNotFoundError) }
            return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
        }

        val file = WasmFsHandleRegister.getHandle(ref.ref) as? File ?: return Outcome.Error(
            KmpFsError.FileNotFoundError,
        )
        return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        if (!supportsOpfs) return false
        val handle = WasmFsHandleRegister.getHandle(ref.ref) as? FileSystemHandle ?: return false
        if (handle is FileSystemFileHandle) handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return false }
        return true
    }
}
