@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.lib.jsTryOutcome
import com.outsidesource.oskitkmp.lib.toArrayBuffer
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.get
import kotlin.coroutines.resume

actual fun platformExternalKmpFs(): IExternalKmpFs = WasmExternalKmpFs()

internal class WasmExternalKmpFs : IExternalKmpFs, IInitializableKmpFs {

    private val fsMixin = WasmKmpFsMixin(KmpFsType.External, { it }, { context != null })
    private var context: KmpFsContext? = null

    override fun init(context: KmpFsContext) { this@WasmExternalKmpFs.context = context }

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        if (supportsFileSystemApi) {
            val mimeTypes = filter?.map { kmpFsMimeTypeToJs(it.mimeType, it.extension) }?.toJsArray()
            val options = showFilePickerOptions(false, mimeTypes = mimeTypes?.let { createMimeTypesObject(it) })
            val handles = showFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
            val handle = handles[0] ?: return Outcome.Ok(null)
            val key = WasmFsHandleRegister.putHandle(handle)
            return Outcome.Ok(
                KmpFsRef(ref = key, name = handle.name, isDirectory = false, fsType = KmpFsType.External),
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
        return Outcome.Ok(KmpFsRef(ref = key, name = file.name, isDirectory = false, fsType = KmpFsType.External))
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

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
                    add(KmpFsRef(ref = key, name = handle.name, isDirectory = false, fsType = KmpFsType.External))
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
                add(KmpFsRef(ref = key, name = file.name, isDirectory = false, fsType = KmpFsType.External))
            }
        }

        return Outcome.Ok(fileRefs)
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        if (supportsFileSystemApi) {
            val handle = showDirectoryPicker(null).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
            val key = WasmFsHandleRegister.putHandle(handle)
            return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = true, fsType = KmpFsType.External))
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

        return Outcome.Ok(KmpFsRef(ref = key, name = directory.name, isDirectory = false, fsType = KmpFsType.External))
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)
        if (!supportsFileSystemApi) return Outcome.Error(KmpFsError.NotSupported)

        val options = showSaveFilePickerOptions(fileName)
        val handle = showSaveFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
        val key = WasmFsHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFsRef(ref = key, name = handle.name, isDirectory = false, fsType = KmpFsType.External))
    }

    override suspend fun saveFile(
        bytes: ByteArray,
        fileName: String,
    ): Outcome<Unit, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)

        jsTryOutcome {
            val url = URL.createObjectURL(Blob(arrayOf(bytes.toArrayBuffer()).toJsArray()))
            val a = document.createElement("a") as HTMLAnchorElement
            a.href = url
            a.download = fileName
            a.click()
            URL.revokeObjectURL(url)
            (0).toJsNumber()
        }.unwrapOrReturn { return Outcome.Error(KmpFsError.Unknown(it.error)) }

        return Outcome.Ok(Unit)
    }

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> =
        Outcome.Error(KmpFsError.NotSupported)

    override suspend fun resolveFile(dir: KmpFsRef, name: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        fsMixin.resolveFile(dir, name, create)

    override suspend fun resolveDirectory(dir: KmpFsRef, name: String, create: Boolean): Outcome<KmpFsRef, KmpFsError> =
        fsMixin.resolveDirectory(dir, name, create)

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> = fsMixin.delete(ref)

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> =
        fsMixin.list(dir, isRecursive)

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> = fsMixin.readMetadata(ref)

    override suspend fun exists(ref: KmpFsRef): Boolean = fsMixin.exists(ref)
}
