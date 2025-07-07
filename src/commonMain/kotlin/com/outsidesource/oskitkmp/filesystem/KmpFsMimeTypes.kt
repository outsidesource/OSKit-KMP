package com.outsidesource.oskitkmp.filesystem

interface IMimeTypeHelper {
    fun getMimeType(extension: String): String?
    fun getMimeTypes(extensions: List<String>): List<String>
    val imageMimeTypes: List<KmpFileMimetype>
    val textMimeTypes: List<KmpFileMimetype>
}

object KmpFsMimeTypes : IMimeTypeHelper by platformMimeTypeHelper()

internal expect fun platformMimeTypeHelper(): IMimeTypeHelper

internal object SharedMimeTypeHelper : IMimeTypeHelper {
    private val extensionToMimeType = mapOf(
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "svg" to "image/svg+xml",
        "bmp" to "image/bmp",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "js" to "application/javascript",
        "html" to "text/html",
        "xml" to "application/xml",
        "txt" to "text/plain"
    )

    override fun getMimeType(extension: String): String? =
        extensionToMimeType[extension.lowercase()]

    override fun getMimeTypes(extensions: List<String>): List<String> =
        extensions.mapNotNull { getMimeType(it) }.distinct()

    override val imageMimeTypes: List<KmpFileMimetype> by lazy {
        listOf("png", "jpg", "jpeg", "svg", "bmp", "gif", "webp").mapNotNull {
            getMimeType(it)?.let { mime -> KmpFileMimetype(it, mime) }
        }
    }

    override val textMimeTypes: List<KmpFileMimetype> by lazy {
        listOf("txt", "html", "xml", "js").mapNotNull {
            getMimeType(it)?.let { mime -> KmpFileMimetype(it, mime) }
        }
    }
}