package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import okio.Sink
import okio.Source
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.js.get
import kotlin.random.Random

actual class KmpFileHandlerContext()

actual class KmpFileHandler : IKmpFileHandler {

    actual override fun init(fileHandlerContext: KmpFileHandlerContext) {}

    actual override suspend fun pickFile(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception> {
        if (supportsFileSystemApi) {
            val mimeTypes = filter?.map { kmpFsMimeTypeToJs(it.mimeType, it.extension) }?.toJsArray()
            val options = showFilePickerOptions(false, mimeTypes = mimeTypes?.let { createMimeTypesObject(it) })
            val handles = showFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
            val handle = handles[0] ?: return Outcome.Ok(null)
            val key = FileHandleRegister.putHandle(handle)
            return Outcome.Ok(KmpFileRef(ref = key, name = handle.name, isDirectory = false))
        }

        val file = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            element.type = "file"
            filter?.let { element.accept = it.joinToString(", ") { it.mimeType } }
            element.click()
            element.addEventListener("cancel") { continuation.resume(Outcome.Ok(null)) }
            element.addEventListener("change") { continuation.resume(Outcome.Ok(element.files?.get(0))) }
        }.unwrapOrReturn { return this }

        if (file == null) return Outcome.Ok(null)
        val key = FileHandleRegister.putHandle(file)
        return Outcome.Ok(KmpFileRef(ref = key, name = file.name, isDirectory = false))
    }

    actual override suspend fun pickFiles(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFileRef>?, Exception> {
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
                    val key = FileHandleRegister.putHandle(handle)
                    add(KmpFileRef(ref = key, name = handle.name, isDirectory = false))
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
        }.unwrapOrReturn { return this }

        if (files == null) return Outcome.Ok(null)
        val fileRefs = buildList {
            for (i in 0 until files.length) {
                val file = files[i] ?: continue
                val key = FileHandleRegister.putHandle(file)
                add(KmpFileRef(ref = key, name = file.name, isDirectory = false))
            }
        }

        return Outcome.Ok(fileRefs)
    }

    actual override suspend fun pickDirectory(startingDir: KmpFileRef?): Outcome<KmpFileRef?, Exception> {
        if (supportsFileSystemApi) {
            val handle = showDirectoryPicker(null).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
            val key = FileHandleRegister.putHandle(handle)
            return Outcome.Ok(KmpFileRef(ref = key, name = handle.name, isDirectory = true))
        }

        val directory = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            element.type = "file"
            element.setAttribute("webkitdirectory", "")
            element.click()
            element.addEventListener("cancel") { continuation.resume(Outcome.Ok(null)) }
            element.addEventListener("change") { continuation.resume(Outcome.Ok(element.files?.get(0))) }
        }.unwrapOrReturn { return this }

        if (directory == null) return Outcome.Ok(null)
        val key = FileHandleRegister.putHandle(directory)

        return Outcome.Ok(KmpFileRef(ref = key, name = directory.name, isDirectory = false))
    }

    actual override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFileRef?,
    ): Outcome<KmpFileRef?, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())

        val options = showSaveFilePickerOptions(fileName)
        val handle = showSaveFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }
        val key = FileHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFileRef(ref = key, name = handle.name, isDirectory = false))
    }

    actual override suspend fun resolveFile(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        val parentHandle = FileHandleRegister.getHandle(dir.ref)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(FileOpenError())
        val handle = parentHandle.getFileHandle(name, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(FileOpenError()) }
        val key = FileHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFileRef(ref = key, name = handle.name, isDirectory = false))
    }

    actual override suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        val parentHandle = FileHandleRegister.getHandle(dir.ref)
            as? FileSystemDirectoryHandle ?: return Outcome.Error(FileOpenError())
        val handle = parentHandle.getDirectoryHandle(name, getHandleOptions(create)).kmpAwaitOutcome()
            .unwrapOrReturn { return Outcome.Error(FileOpenError()) }
        val key = FileHandleRegister.putHandle(handle)
        return Outcome.Ok(KmpFileRef(ref = key, name = handle.name, isDirectory = true))
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception> {
        return Outcome.Error(NotSupportedError())
    }

    actual override suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        val handle = FileHandleRegister.getHandle(ref.ref) as? FileSystemHandle ?: return Outcome.Error(FileOpenError())
        handle.remove(removeOptions(true)).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(FileDeleteError()) }
        return Outcome.Ok(Unit)
    }

    actual override suspend fun list(dir: KmpFileRef, isRecursive: Boolean): Outcome<List<KmpFileRef>, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val handle = FileHandleRegister.getHandle(dir.ref) as? FileSystemDirectoryHandle
                ?: return Outcome.Error(FileListError())

            if (!isRecursive) {
                val list = handle.entries().map {
                    val key = FileHandleRegister.putHandle(it)
                    KmpFileRef(ref = key, name = it.name, isDirectory = it is FileSystemDirectoryHandle)
                }
                return Outcome.Ok(list)
            }

            val list = handle.entries().flatMap {
                buildList {
                    val key = FileHandleRegister.putHandle(it)
                    val childHandle =
                        KmpFileRef(ref = key, name = it.name, isDirectory = it is FileSystemDirectoryHandle)

                    add(childHandle)

                    if (it is FileSystemDirectoryHandle) {
                        addAll(list(childHandle).unwrapOrNull() ?: emptyList())
                    }
                }
            }

            return Outcome.Ok(list)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun readMetadata(ref: KmpFileRef): Outcome<KmpFileMetadata, Exception> {
        if (supportsFileSystemApi) {
            val handle = FileHandleRegister.getHandle(ref.ref)
                as? FileSystemFileHandle ?: return Outcome.Error(FileNotFoundError())
            val file = handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(FileNotFoundError()) }
            return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
        }

        val file = FileHandleRegister.getHandle(ref.ref) as? File ?: return Outcome.Error(FileNotFoundError())
        return Outcome.Ok(KmpFileMetadata(size = file.size.toDouble().toLong()))
    }

    actual override suspend fun exists(ref: KmpFileRef): Boolean {
        if (!supportsFileSystemApi) return false
        FileHandleRegister.getHandle(ref.ref) as? FileSystemHandle ?: return false
        return true
    }
}

actual suspend fun KmpFileRef.source(): Outcome<Source, Exception> {
    if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
    val file = getFile().unwrapOrReturn { return this }
    return createSourceFromJsFile(file)
}

actual suspend fun KmpFileRef.asyncSource(): Outcome<IKmpFsAsyncSource, Exception> {
    if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
    val file = getFile().unwrapOrReturn { return this }
    return Outcome.Ok(KmpFsAsyncSource(file))
}

actual suspend fun KmpFileRef.sink(mode: KmpFileWriteMode): Outcome<Sink, Exception> {
    if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
    if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())

    val handle = FileHandleRegister.getHandle(ref)
        as? FileSystemFileHandle ?: return Outcome.Error(FileNotFoundError())
    val writable = handle.createWritable(createWritableOptions(mode == KmpFileWriteMode.Append)).kmpAwaitOutcome()
        .unwrapOrReturn { return Outcome.Error(FileOpenError()) }
    // TODO
    return Outcome.Error(NotSupportedError())
}

private suspend fun KmpFileRef.getFile(): Outcome<File, Exception> {
    val file = if (supportsFileSystemApi) {
        val handle = FileHandleRegister.getHandle(ref)
            as? FileSystemFileHandle ?: return Outcome.Error(FileNotFoundError())
        handle.getFile().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(FileOpenError()) }
    } else {
        FileHandleRegister.getHandle(ref) as? File ?: return Outcome.Error(FileNotFoundError())
    }

    return Outcome.Ok(file)
}

private object FileHandleRegister {
    private val lock = SynchronizedObject()
    private val handles: MutableMap<String, Any> = mutableMapOf()
    private val counter = atomic(Random(Clock.System.now().toEpochMilliseconds()).nextLong())

    @OptIn(ExperimentalStdlibApi::class)
    private fun createUniqueKey(): String =
        Clock.System.now().epochSeconds.toHexString().takeLast(10) + counter.incrementAndGet().toHexString()

    fun putHandle(file: Any): String {
        val key = createUniqueKey()
        synchronized(lock) { handles[key] = file }
        return key
    }

    fun getHandle(key: String): Any? = synchronized(lock) { handles[key] }
}
