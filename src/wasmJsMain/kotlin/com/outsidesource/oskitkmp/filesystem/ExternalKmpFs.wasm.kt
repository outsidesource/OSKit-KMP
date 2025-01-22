package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.lib.toArrayBuffer
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.get
import kotlin.coroutines.resume

actual fun platformExternalKmpFs(): IExternalKmpFs = WasmExternalKmpFs()

internal class WasmExternalKmpFs : IExternalKmpFs, IInitializableKmpFs {

    override fun init(fileHandlerContext: KmpFsContext) {}

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (supportsFileSystemApi) {
            val mimeTypes = filter?.map { kmpFsMimeTypeToJs(it.mimeType, it.extension) }?.toJsArray()
            val options = showFilePickerOptions(false, mimeTypes = mimeTypes?.let { createMimeTypesObject(it) })
            val handles = showFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
            val handle = handles[0] ?: return Outcome.Ok(null)
            val key = WasmFsHandleRegister.putHandle(handle)
            return Outcome.Ok(
                KmpFsRef(ref = key, name = handle.name, isDirectory = false, type = KmpFsType.External),
            )
        }

        val file = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            element.type = "file"
            filter?.let { element.accept = it.joinToString(", ") { it.mimeType } }
            element.click()
            element.addEventListener("cancel") { continuation.resume(Outcome.Ok(null)) }
            element.addEventListener("change") { continuation.resume(Outcome.Ok(element.files?.get(0))) }
        }.unwrapOrReturn { return it }

        if (file == null) return Outcome.Ok(null)
        val key = WasmFsHandleRegister.putHandle(file)
        return Outcome.Ok(KmpFsRef(ref = key, name = file.name, isDirectory = false, type = KmpFsType.External))
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        if (supportsFileSystemApi) {
            val mimeTypes = filter?.map { kmpFsMimeTypeToJs(it.mimeType, it.extension) }?.toJsArray()
            val options = showFilePickerOptions(true, mimeTypes = mimeTypes?.let { createMimeTypesObject(it) })
            val handles = showFilePicker(options)
                .kmpAwaitOutcome()
                .unwrapOrReturn { return Outcome.Ok(null) }
            if (handles.length == 0) return Outcome.Ok(null)
            val refs = buildList {
                for (i in 0 until handles.length) {
                    val handle = handles[i] ?: continue
                    val key = WasmFsHandleRegister.putHandle(handle)
                    add(KmpFsRef(ref = key, name = handle.name, isDirectory = false, type = KmpFsType.External))
                }
            }
            return Outcome.Ok(refs)
        }

        val files = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            element.type = "file"
            filter?.let { element.accept = it.joinToString(", ") { it.mimeType } }
            element.setAttribute("multiple", "")
            element.click()
            element.addEventListener("cancel") { continuation.resume(Outcome.Ok(null)) }
            element.addEventListener("change") { continuation.resume(Outcome.Ok(element.files)) }
        }.unwrapOrReturn { return it }

        if (files == null) return Outcome.Ok(null)
        val fileRefs = buildList {
            for (i in 0 until files.length) {
                val file = files[i] ?: continue
                val key = WasmFsHandleRegister.putHandle(file)
                add(KmpFsRef(ref = key, name = file.name, isDirectory = false, type = KmpFsType.External))
            }
        }

        return Outcome.Ok(fileRefs)
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        if (supportsFileSystemApi) {
            val handle = showDirectoryPicker(null).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
            val key = WasmFsHandleRegister.putHandle(handle)
            return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = true, type = KmpFsType.External))
        }

        val directory = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            element.type = "file"
            element.setAttribute("webkitdirectory", "")
            element.click()
            element.addEventListener("cancel") { continuation.resume(Outcome.Ok(null)) }
            element.addEventListener("change") { continuation.resume(Outcome.Ok(element.files?.get(0))) }
        }.unwrapOrReturn { return it }

        if (directory == null) return Outcome.Ok(null)
        val key = WasmFsHandleRegister.putHandle(directory)

        return Outcome.Ok(KmpFsRef(ref = key, name = directory.name, isDirectory = false, type = KmpFsType.External))
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)

        val options = showSaveFilePickerOptions(fileName)
        val handle = showSaveFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = false, type = KmpFsType.External))
    }

    override suspend fun saveFile(
        bytes: ByteArray,
        fileName: String,
    ): Outcome<Unit, KmpFsError> {
        val url = URL.createObjectURL(Blob(arrayOf(bytes.toArrayBuffer()).toJsArray()))
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = url
        a.download = fileName
        a.click()
        URL.revokeObjectURL(url)
        return Outcome.Ok(Unit)
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)
        val parentHandle = WasmFsHandleRegister.getHandle(dir.ref)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        val handle = parentHandle.getFileHandle(fileName, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(if (create) KmpFsError.RefNotCreated else KmpFsError.RefNotFound) }
        if (handle.kind == FileSystemHandleKind.Directory.value) return Outcome.Error(KmpFsError.RefExistsAsDirectory)

        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = false, type = KmpFsType.External))
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)
        val parentHandle = WasmFsHandleRegister.getHandle(dir.ref, WasmFsHandleAccessMode.Write)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
        val handle = parentHandle.getDirectoryHandle(name, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(if (create) KmpFsError.RefNotCreated else KmpFsError.RefNotFound) }
        if (handle.kind == FileSystemHandleKind.File.value) return Outcome.Error(KmpFsError.RefExistsAsFile)

        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = true, type = KmpFsType.External))
    }

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        return Outcome.Error(KmpFsError.NotSupported)
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)
        val handle = WasmFsHandleRegister.getHandle(ref.ref, WasmFsHandleAccessMode.Write) as? FileSystemHandle
            ?: return Outcome.Error(KmpFsError.InvalidRef)
        handle
            .remove(removeOptions(true))
            .kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
        return Outcome.Ok(Unit)
    }

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> {
        if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val handle = WasmFsHandleRegister
                .getHandle(dir.ref, WasmFsHandleAccessMode.Read) as? FileSystemDirectoryHandle
                ?: return Outcome.Error(KmpFsError.InvalidRef)

            if (!isRecursive) {
                val list = handle.entries().map {
                    val key = WasmFsHandleRegister.putHandle(it)
                    KmpFsRef(
                        ref = key,
                        name = it.name,
                        isDirectory = it is FileSystemDirectoryHandle,
                        type = KmpFsType.External,
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
                            type = KmpFsType.External,
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
        if (supportsFileSystemApi) {
            val handle = WasmFsHandleRegister.getHandle(ref.ref)
                as? FileSystemFileHandle ?: return Outcome.Error(KmpFsError.InvalidRef)
            val file = handle
                .getFile()
                .kmpAwaitOutcome()
                .unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }
            return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
        }

        val file = WasmFsHandleRegister.getHandle(ref.ref) as? File
            ?: return Outcome.Error(KmpFsError.InvalidRef)
        return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        if (!supportsFileSystemApi) return false
        val handle = WasmFsHandleRegister.getHandle(ref.ref) as? FileSystemHandle ?: return false
        if (handle is FileSystemFileHandle) handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return false }
        return true
    }
}
