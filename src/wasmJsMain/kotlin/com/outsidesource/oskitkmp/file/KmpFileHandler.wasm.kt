package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
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
import kotlin.js.Promise
import kotlin.random.Random

actual class KmpFileHandlerContext()

/***
 * I'm not going to be able to implement persistence of file refs due to browser support of the file access API.
 * Only chrome and its derivatives will support picking a save file in wasm.
 * All picked files will live in memory for the lifetime of the session.
 */
actual class KmpFileHandler : IKmpFileHandler {

    actual override fun init(fileHandlerContext: KmpFileHandlerContext) {}

    actual override suspend fun pickFile(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception> {
        if (supportsFileSystemApi) {
            val files = showFilePicker(null).kmpAwaitOutcome()
            return Outcome.Error(Exception(""))
        }

        val file = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            // TODO: Implement filter
            element.type = "file"
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
            val files = showFilePicker(showFilePickerOptions(true)).kmpAwaitOutcome()
            return Outcome.Error(Exception(""))
        }

        val files = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            // TODO: Implement filter
            element.type = "file"
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
            val directory = showDirectoryPicker(null).kmpAwaitOutcome().unwrapOrReturn {
                return Outcome.Error(
                    Exception(""),
                )
            }
            directory.entries().forEach { handle ->
                println(handle.name)
            }
            return Outcome.Error(Exception(""))
        }

        val directory = suspendCancellableCoroutine { continuation ->
            val element = document.createElement("input") as HTMLInputElement
            element.type = "file"
            element.setAttribute("webkitdirectory", "")
            element.click()
            // TODO: Implement starting directory
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
        val file = showSaveFilePicker(options).kmpAwaitOutcome().unwrapOrReturn { return Outcome.Ok(null) }

        return Outcome.Error(Exception(""))
    }

    actual override suspend fun resolveFile(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        return Outcome.Error(NotSupportedError())
    }

    actual override suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        return Outcome.Error(NotSupportedError())
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        return Outcome.Error(NotSupportedError())
    }

    actual override suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        return Outcome.Error(NotSupportedError())
    }

    actual override suspend fun list(dir: KmpFileRef, isRecursive: Boolean): Outcome<List<KmpFileRef>, Exception> {
        if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
        return Outcome.Error(NotSupportedError())
    }

    actual override suspend fun readMetadata(ref: KmpFileRef): Outcome<KMPFileMetadata, Exception> {
        val file = FileHandleRegister.getHandle(ref.ref) as? File ?: return Outcome.Error(FileNotFoundError())
        return Outcome.Ok(KMPFileMetadata(size = file.size.toInt().toLong()))
    }

    actual override suspend fun exists(ref: KmpFileRef): Boolean {
        if (!supportsFileSystemApi) return false
        return false
    }
}

actual suspend fun KmpFileRef.source(): Outcome<Source, Exception> {
    if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
    val file = FileHandleRegister.getHandle(ref) as? File ?: return Outcome.Error(FileNotFoundError())
    return createSourceFromJsFile(file)
}

actual suspend fun KmpFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    if (!supportsFileSystemApi) return Outcome.Error(NotSupportedError())
    // TODO: I might be able to implement this in Chrome
    return Outcome.Error(NotSupportedError())
}

private object FileHandleRegister {
    private val lock = SynchronizedObject()
    private val handles: MutableMap<String, Any> = mutableMapOf()
    private val counter = atomic(Random(Clock.System.now().toEpochMilliseconds()).nextLong())

    @OptIn(ExperimentalStdlibApi::class)
    private fun createUniqueKey(): String =
        Clock.System.now().epochSeconds.toHexString().takeLast(10) + counter.incrementAndGet().toHexString()

    fun putHandle(file: File): String {
        val key = createUniqueKey()
        synchronized(lock) { handles[key] = file }
        return key
    }

    fun getHandle(key: String): Any? = synchronized(lock) { handles[key] }
}

/**
 * External
 */
private val supportsFileSystemApi: Boolean = js("""window.hasOwnProperty("showOpenFilePicker")""")

private fun showSaveFilePicker(options: JsAny?): Promise<FileSystemFileHandle> =
    js("""window.showSaveFilePicker(options)""")
private fun showSaveFilePickerOptions(suggestedName: String): JsAny = js("""({suggestedName: suggestedName})""")

private fun showFilePicker(options: JsAny?): Promise<FileSystemDirectoryHandle> =
    js("""window.showOpenFilePicker(options)""")
private fun showFilePickerOptions(multiple: Boolean): JsAny = js("""({multiple: multiple})""")

private fun showDirectoryPicker(options: JsAny?): Promise<FileSystemDirectoryHandle> =
    js("""window.showDirectoryPicker(options)""")
private fun showDirectoryPickerOptions(mode: String): JsAny = js("""({mode: mode})""")

open external class FileSystemHandle : JsAny {
    val kind: String
    val name: String
    fun queryPermission(descriptor: JsAny?): Promise<JsString>
    fun remove(options: JsAny?): Promise<JsAny?>
}
external class FileSystemFileHandle : FileSystemHandle {
    fun getFile(): Promise<File>
    fun createWritable(options: JsAny?): Promise<FileSystemWritableFileStream>
}
external class FileSystemDirectoryHandle : FileSystemHandle {
    fun getDirectoryHandle(name: String, options: JsAny?): Promise<FileSystemDirectoryHandle>
    fun getFileHandle(name: String, options: JsAny?): Promise<FileSystemFileHandle>
    fun removeEntry(name: String, options: JsAny?): Promise<JsAny?>
}

suspend fun FileSystemDirectoryHandle.entries(): List<FileSystemHandle> = buildList {
    directoryEntries(this@entries) { _, handle -> add(handle) }.kmpAwaitOutcome()
}

private fun directoryEntries(
    handle: FileSystemDirectoryHandle,
    add: (name: String, value: FileSystemHandle) -> Unit,
): Promise<JsAny?> = js(
    """(
    new Promise(async (res, rej) => {
        for await (const [key, value] of handle.entries()) {
            add(key, value)
        }
        res()
    })
    )""",
)

// TODO: Make helper for AsyncGenerator

external class FileSystemWritableFileStream : JsAny
