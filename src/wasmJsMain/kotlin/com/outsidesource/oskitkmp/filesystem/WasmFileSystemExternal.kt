package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.File
import kotlin.js.Promise

internal val supportsFileSystemApi: Boolean = js("""window.hasOwnProperty("showOpenFilePicker")""")

internal fun showSaveFilePicker(options: JsAny?): Promise<FileSystemFileHandle> =
    js("""window.showSaveFilePicker(options)""")

internal fun showSaveFilePickerOptions(suggestedName: String): JsAny = js("""({suggestedName: suggestedName})""")

internal fun showFilePicker(options: JsAny?): Promise<JsArray<FileSystemDirectoryHandle>> =
    js("""window.showOpenFilePicker(options)""")

internal fun showFilePickerOptions(multiple: Boolean, mimeTypes: JsAny?): JsAny = js(
    """({
        "multiple": multiple, 
        "types": mimeTypes ?? [],
    })""",
)

internal fun createMimeTypesObject(mimeTypes: JsArray<JsAny>): JsAny = js(
    """([{ 
        accept: (() => {
            const obj = {}
            for (const type of mimeTypes) {
                obj[type.type] = [type.extension]
            }
            return obj
        })()
    }])""",
)

internal fun kmpFsMimeTypeToJs(mimeType: String, extension: String): JsAny = js(
    """({
        type: mimeType.toString(),
        extension: "." + extension
    })""",
)

internal fun showDirectoryPicker(options: JsAny?): Promise<FileSystemDirectoryHandle> =
    js("""window.showDirectoryPicker(options)""")

internal fun showDirectoryPickerOptions(mode: String): JsAny = js("""({mode: mode})""")

internal open external class FileSystemHandle : JsAny {
    val kind: String
    val name: String
    fun queryPermission(descriptor: JsAny?): Promise<JsString>
    fun requestPermission(descriptor: JsAny?): Promise<JsString>
    fun remove(options: JsAny?): Promise<JsAny?>
}

internal fun permissionOptions(mode: String): JsAny = js("""({"mode": mode})""")
internal fun removeOptions(recursive: Boolean): JsAny = js("""({"recursive": recursive})""")

internal external class FileSystemFileHandle : FileSystemHandle {
    fun getFile(): Promise<File>
    fun createWritable(options: JsAny?): Promise<FileSystemWritableFileStream>
}

internal fun createWritableOptions(append: Boolean): JsAny = js(
    """({keepExistingData: append === true})""",
)

internal external class FileSystemDirectoryHandle : FileSystemHandle {
    fun getDirectoryHandle(name: String, options: JsAny?): Promise<FileSystemDirectoryHandle>
    fun getFileHandle(name: String, options: JsAny?): Promise<FileSystemFileHandle>
    fun removeEntry(name: String, options: JsAny?): Promise<JsAny?>
}

internal fun getHandleOptions(create: Boolean): JsAny = js("""({"create": create})""")

internal suspend fun FileSystemDirectoryHandle.entries(): List<FileSystemHandle> = buildList {
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

internal external class FileSystemWritableFileStream : JsAny {
    fun write(data: JsAny): Promise<JsAny?>
    fun seek(position: JsNumber): Promise<JsAny?>
    fun truncate(size: JsNumber): Promise<JsAny?>
    fun close(): Promise<JsAny?>
}

fun writeOptions(
    type: String,
    data: ArrayBuffer,
    position: JsNumber? = null,
    size: JsNumber? = null,
): JsAny = js(
    """({
        "type": type,
        "data": data,
        ...(() => { if (position) return { "position": position } })(),
        ...(() => { if (size) return { "size": size } })(),
    })""",
)

internal external class Blob {
    val size: JsNumber
    val type: String
    val isClosed: Boolean
    fun arrayBuffer(): Promise<ArrayBuffer>
    fun slice(
        start: JsNumber = definedExternally,
        end: JsNumber = definedExternally,
        contentType: String = definedExternally,
    ): Blob
    fun close()

    constructor(parts: JsArray<ArrayBuffer>)
}

internal external object URL {
    fun createObjectURL(blob: Blob): String
    fun revokeObjectURL(url: String)
}
