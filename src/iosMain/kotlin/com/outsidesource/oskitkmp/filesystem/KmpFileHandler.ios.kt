package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.Deferrer
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import io.ktor.util.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import platform.Foundation.*
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

actual data class KmpFileHandlerContext(val rootController: UIViewController)

actual class KmpFileHandler : IKmpFileHandler {
    private var context: KmpFileHandlerContext? = null

    private val documentPickerDelegate = IOSPickerDelegate()

    private val directoryPickerDelegate = IOSPickerDelegate()
    private val directoryPickerViewController = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeFolder),
    ).apply {
        delegate = directoryPickerDelegate
        allowsMultipleSelection = false
    }

    actual override fun init(fileHandlerContext: KmpFileHandlerContext) {
        context = fileHandlerContext
    }

    actual override suspend fun pickFile(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception> {
        try {
            val context = context ?: return Outcome.Error(NotInitializedError())

            withContext(Dispatchers.Main) {
                val openFilePicker = UIDocumentPickerViewController(
                    forOpeningContentTypes = filter?.map { UTType.typeWithFilenameExtension(it.extension) }
                        ?: listOf(UTTypeItem),
                ).apply {
                    delegate = documentPickerDelegate
                    allowsMultipleSelection = false
                    shouldShowFileExtensions = true
                }

                openFilePicker.directoryURL = startingDir?.toNSURL()

                context.rootController.presentViewController(
                    viewControllerToPresent = openFilePicker,
                    animated = true,
                    completion = null,
                )
            }

            val url = documentPickerDelegate.resultFlow.firstOrNull()?.firstOrNull() ?: return Outcome.Ok(null)
            return Outcome.Ok(url.toKMPFileRef(isDirectory = false))
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    actual override suspend fun pickFiles(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFileRef>?, Exception> {
        try {
            val context = context ?: return Outcome.Error(NotInitializedError())

            withContext(Dispatchers.Main) {
                val openFilePicker = UIDocumentPickerViewController(
                    forOpeningContentTypes = filter?.map { UTType.typeWithFilenameExtension(it.extension) }
                        ?: listOf(UTTypeItem),
                ).apply {
                    delegate = documentPickerDelegate
                    allowsMultipleSelection = true
                    shouldShowFileExtensions = true
                }

                openFilePicker.directoryURL = startingDir?.toNSURL()

                context.rootController.presentViewController(
                    viewControllerToPresent = openFilePicker,
                    animated = true,
                    completion = null,
                )
            }

            val urls = documentPickerDelegate.resultFlow.firstOrNull() ?: return Outcome.Ok(null)
            val refs = urls.map { it.toKMPFileRef(false) }

            return Outcome.Ok(refs)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    actual override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFileRef?,
    ): Outcome<KmpFileRef?, Exception> {
        val directory = pickDirectory(startingDir).unwrapOrReturn { return this } ?: return Outcome.Ok(null)
        return resolveFile(directory, fileName, create = true)
    }

    actual override suspend fun pickDirectory(startingDir: KmpFileRef?): Outcome<KmpFileRef?, Exception> {
        try {
            val context = context ?: return Outcome.Error(NotInitializedError())

            withContext(Dispatchers.Main) {
                directoryPickerViewController.directoryURL = startingDir?.toNSURL()
                context.rootController.presentViewController(
                    viewControllerToPresent = directoryPickerViewController,
                    animated = true,
                    completion = null,
                )
            }

            val url = directoryPickerDelegate.resultFlow.firstOrNull()?.firstOrNull() ?: return Outcome.Ok(null)
            return Outcome.Ok(url.toKMPFileRef(isDirectory = true))
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    actual override suspend fun resolveFile(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        val deferrer = Deferrer()

        return try {
            val directoryUrl = dir.toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())
            directoryUrl.startAccessingSecurityScopedResource()
            deferrer.defer { directoryUrl.stopAccessingSecurityScopedResource() }

            val url = directoryUrl.URLByAppendingPathComponent(name) ?: return Outcome.Error(FileCreateError())
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists && !create) return Outcome.Error(FileNotFoundError())
            if (!exists && create) {
                val created = NSFileManager.defaultManager.createFileAtPath(
                    path = url.path ?: "",
                    contents = null,
                    attributes = null,
                )
                if (!created) return Outcome.Error(FileCreateError())
            }

            Outcome.Ok(url.toKMPFileRef(isDirectory = false))
        } catch (e: Exception) {
            Outcome.Error(e)
        } finally {
            deferrer.run()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual override suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        val deferrer = Deferrer()

        return try {
            val directoryUrl = dir.toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())
            directoryUrl.startAccessingSecurityScopedResource()
            deferrer.defer { directoryUrl.stopAccessingSecurityScopedResource() }

            val url = directoryUrl.URLByAppendingPathComponent(name) ?: return Outcome.Error(FileCreateError())
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists && !create) return Outcome.Error(FileNotFoundError())
            if (!exists && create) {
                val created = NSFileManager.defaultManager.createDirectoryAtPath(
                    path = url.path ?: "",
                    withIntermediateDirectories = false,
                    attributes = null,
                    error = null,
                )
                if (!created) return Outcome.Error(FileCreateError())
            }

            Outcome.Ok(url.toKMPFileRef(isDirectory = true))
        } catch (e: Exception) {
            Outcome.Error(e)
        } finally {
            deferrer.run()
        }
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception> {
        return try {
            val url = NSURL(fileURLWithPath = path)
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists) return Outcome.Error(FileNotFoundError())

            Outcome.Ok(url.toKMPFileRef(isDirectory = url.hasDirectoryPath))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual override suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception> {
        val deferrer = Deferrer()

        return try {
            val url = ref.toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())
            url.startAccessingSecurityScopedResource()
            deferrer.defer { url.stopAccessingSecurityScopedResource() }

            val deleteSuccess = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                NSFileManager.defaultManager.removeItemAtPath(url.path ?: "", error.ptr)
            }

            return if (deleteSuccess) Outcome.Ok(Unit) else Outcome.Error(FileDeleteError())
        } catch (e: Exception) {
            Outcome.Error(e)
        } finally {
            deferrer.run()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual override suspend fun list(dir: KmpFileRef, isRecursive: Boolean): Outcome<List<KmpFileRef>, Exception> {
        val deferrer = Deferrer()

        return try {
            val list = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val directoryUrl = dir.toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())

                directoryUrl.startAccessingSecurityScopedResource()
                deferrer.defer { directoryUrl.stopAccessingSecurityScopedResource() }

                val paths = NSFileManager.defaultManager.contentsOfDirectoryAtPath(directoryUrl.path ?: "", error.ptr)
                    ?: return Outcome.Error(FileListError())

                if (!isRecursive) {
                    val list = paths.mapNotNull {
                        val path = it as? String ?: return@mapNotNull null
                        directoryUrl.URLByAppendingPathComponent(path)?.toKMPFileRef(isDirectory = false)
                    }
                    return Outcome.Ok(list)
                }

                paths.flatMap {
                    val path = it as? String ?: return@flatMap emptyList()
                    val url = directoryUrl.URLByAppendingPathComponent(path) ?: return@flatMap emptyList()

                    buildList {
                        val file = url.toKMPFileRef(isDirectory = false)
                        add(file)

                        if (url.hasDirectoryPath) {
                            addAll(list(file).unwrapOrNull() ?: emptyList())
                        }
                    }
                }
            }

            Outcome.Ok(list)
        } catch (e: Exception) {
            Outcome.Error(e)
        } finally {
            deferrer.run()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual override suspend fun readMetadata(ref: KmpFileRef): Outcome<KmpFileMetadata, Exception> {
        val deferrer = Deferrer()

        try {
            val attributes = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val url = ref.toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())

                url.startAccessingSecurityScopedResource()
                deferrer.defer { url.stopAccessingSecurityScopedResource() }

                val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(url.path ?: "", error.ptr) ?: run {
                    url.stopAccessingSecurityScopedResource()
                    return Outcome.Error(FileMetadataError())
                }
                attrs
            }

            val size = attributes[NSFileSize] ?: return Outcome.Error(FileMetadataError())

            return Outcome.Ok(KmpFileMetadata(size = size as Long))
        } catch (e: Exception) {
            return Outcome.Error(e)
        } finally {
            deferrer.run()
        }
    }

    actual override suspend fun exists(ref: KmpFileRef): Boolean {
        val deferrer = Deferrer()

        return try {
            val url = ref.toNSURL() ?: return false

            url.startAccessingSecurityScopedResource()
            deferrer.defer { url.stopAccessingSecurityScopedResource() }

            NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")
        } catch (e: Exception) {
            false
        } finally {
            deferrer.run()
        }
    }
}

private class IOSPickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {
    val resultFlow = MutableSharedFlow<List<NSURL>?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        coroutineScope.launch {
            resultFlow.emit(null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        coroutineScope.launch {
            resultFlow.emit(didPickDocumentsAtURLs as List<NSURL>)
        }
    }
}

actual suspend fun KmpFileRef.source(): Outcome<IKmpFsAsyncSource, Exception> {
    val deferrer = Deferrer()

    return try {
        if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
        val url = toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())

        url.startAccessingSecurityScopedResource()
        deferrer.defer { url.stopAccessingSecurityScopedResource() }

        val source = NSInputStream(uRL = url).source()
        Outcome.Ok(KmpFsOkIoAsyncSource(source))
    } catch (e: Exception) {
        Outcome.Error(e)
    } finally {
        deferrer.run()
    }
}

actual suspend fun KmpFileRef.sink(mode: KmpFileWriteMode): Outcome<IKmpFsAsyncSink, Exception> {
    val deferrer = Deferrer()

    return try {
        if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
        val url = toNSURL() ?: return Outcome.Error(KmpFileNsUrlError())

        url.startAccessingSecurityScopedResource()
        deferrer.defer { url.stopAccessingSecurityScopedResource() }

        val sink = NSOutputStream(uRL = url, append = mode == KmpFileWriteMode.Append).sink()
        Outcome.Ok(KmpFsOkIoAsyncSink(sink))
    } catch (e: Exception) {
        Outcome.Error(e)
    } finally {
        deferrer.run()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.toKMPFileRef(isDirectory: Boolean): KmpFileRef {
    startAccessingSecurityScopedResource()

    val ref = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val bookmarkData = bookmarkDataWithOptions(
            options = NSURLBookmarkCreationMinimalBookmark,
            includingResourceValuesForKeys = null,
            relativeToURL = null,
            error = error.ptr,
        )
        bookmarkData?.bytes?.readBytes(bookmarkData.length.toInt())?.encodeBase64()
    }

    stopAccessingSecurityScopedResource()

    return KmpFileRef(
        ref = ref ?: "",
        name = path?.split("/")?.lastOrNull() ?: "",
        isDirectory = isDirectory,
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun KmpFileRef.toNSURL(): NSURL? {
    val data = NSMutableData()
    ref.decodeBase64Bytes().usePinned {
        data.appendBytes(it.addressOf(0).reinterpret(), it.get().size.convert())
    }

    return memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val isStale = alloc<BooleanVar>()

        val url = NSURL(
            byResolvingBookmarkData = data,
            options = NSURLBookmarkResolutionWithoutUI,
            relativeToURL = null,
            bookmarkDataIsStale = isStale.ptr,
            error = error.ptr,
        )

        if (error.value != null) return null
        if (isStale.value) return null

        url
    }
}

class KmpFileNsUrlError : Exception("Error converting KMPFileRef to NSURL")
