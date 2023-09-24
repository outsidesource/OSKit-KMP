package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import io.ktor.util.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import okio.Sink
import okio.Source
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

actual data class KMPFileHandlerContext(val rootController: UIViewController)

class IOSKMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null

    private val documentPickerDelegate = IOSPickerDelegate()
    private val documentPickerViewController = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeItem)
    ).apply {
        delegate = documentPickerDelegate
        allowsMultipleSelection = false
    }

    private val directoryPickerDelegate = IOSPickerDelegate()
    private val directoryPickerViewController = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeFolder)
    ).apply {
        delegate = directoryPickerDelegate
        allowsMultipleSelection = false
    }

    private val createFileDelegate = IOSDocumentBrowserDelegate()
    private val createFilePickerViewController = UIDocumentBrowserViewController(
        forOpeningContentTypes = listOf(UTTypeItem)
    ).apply {
        delegate = createFileDelegate
        allowsDocumentCreation = true
    }

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?
    ): Outcome<KMPFileRef?, Exception> {
        try {
            val context = context ?: return Outcome.Error(NotInitializedException())

            withContext(Dispatchers.Main) {
                context.rootController.presentViewController(
                    viewControllerToPresent = documentPickerViewController,
                    animated = true,
                    completion = null
                )
            }

            val url = documentPickerDelegate.resultFlow.firstOrNull() ?: return Outcome.Ok(null)
            return Outcome.Ok(url.toKMPFileRef())
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun pickSaveFile(defaultName: String?): Outcome<KMPFileRef?, Exception> {
        try {
            val context = context ?: return Outcome.Error(NotInitializedException())

            // TODO: Need to be able to close this view controller since it doesn't look like it's cancellable
            withContext(Dispatchers.Main) {
                context.rootController.presentViewController(
                    viewControllerToPresent = createFilePickerViewController,
                    animated = true,
                    completion = null,
                )
            }
//        createFilePickerViewController.dismissViewControllerAnimated(true) {}

            return Outcome.Ok(null)
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun pickFolder(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        try {
            val context = context ?: return Outcome.Error(NotInitializedException())

            withContext(Dispatchers.Main) {
                context.rootController.presentViewController(
                    viewControllerToPresent = directoryPickerViewController,
                    animated = true,
                    completion = null
                )
            }

            val url = directoryPickerDelegate.resultFlow.firstOrNull() ?: return Outcome.Ok(null)
            return Outcome.Ok(url.toKMPFileRef())
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun resolveFile(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        return try {
            val directoryUrl = dir.toNSURL()
            val url = directoryUrl.URLByAppendingPathComponent(name) ?: return Outcome.Error(FileCreateException())
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists && !create) return Outcome.Error(FileNotFoundException())
            if (!exists && create) {
                val created = NSFileManager.defaultManager.createFileAtPath(
                    path = url.path ?: "",
                    contents = null,
                    attributes = null
                )
                if (!created) return Outcome.Error(FileCreateException())
            }

            Outcome.Ok(url.toKMPFileRef())
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        return try {
            val directoryUrl = dir.toNSURL()
            val url = directoryUrl.URLByAppendingPathComponent(name) ?: return Outcome.Error(FileCreateException())
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists && !create) return Outcome.Error(FileNotFoundException())
            if (!exists && create) {
                val created = NSFileManager.defaultManager.createDirectoryAtPath(
                    path = url.path ?: "",
                    withIntermediateDirectories = false,
                    attributes = null,
                    error = null,
                )
                if (!created) return Outcome.Error(FileCreateException())
            }

            Outcome.Ok(url.toKMPFileRef())
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun rename(ref: KMPFileRef, name: String): Outcome<KMPFileRef, Exception> {
        return try {
            val url = ref.toNSURL()
            val parentUrl = url.URLByDeletingLastPathComponent
                ?: return Outcome.Error(FileRenameException())
            val newUrl = parentUrl.URLByAppendingPathComponent(name) ?: return Outcome.Error(FileRenameException())

            val renameSuccess = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                NSFileManager.defaultManager.moveItemAtPath(
                    srcPath = url.path ?: "",
                    toPath = newUrl.path ?: "",
                    error = error.ptr
                )
            }

            if (!renameSuccess) return Outcome.Error(FileRenameException())

            Outcome.Ok(newUrl.toKMPFileRef())
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception> {
        return try {
            val url = ref.toNSURL()
            val deleteSuccess = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                NSFileManager.defaultManager.removeItemAtPath(url.path ?: "", error.ptr)
            }

            return if (deleteSuccess) Outcome.Ok(Unit) else Outcome.Error(FileDeleteException())
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun list(dir: KMPFileRef, isRecursive: Boolean): Outcome<List<KMPFileRef>, Exception> {
        return try {
            val list = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val directoryUrl = dir.toNSURL()
                val paths = NSFileManager.defaultManager.contentsOfDirectoryAtPath(directoryUrl.path ?: "", error.ptr)
                    ?: return Outcome.Error(FileListException())

                if (!isRecursive) {
                    val list = paths.mapNotNull {
                        val path = it as? String ?: return@mapNotNull null
                        directoryUrl.URLByAppendingPathComponent(path)?.toKMPFileRef()
                    }
                    return Outcome.Ok(list)
                }

                paths.flatMap {
                    val path = it as? String ?: return@flatMap emptyList()
                    val url = directoryUrl.URLByAppendingPathComponent(path) ?: return@flatMap emptyList()

                    buildList {
                        val file = url.toKMPFileRef()
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
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception> {
        try {
            val attributes = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val url = ref.toNSURL()
                val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(url.path ?: "", error.ptr)
                    ?: return Outcome.Error(FileMetadataException())
                attrs
            }

            val size = attributes[NSFileSize] ?: return Outcome.Error(FileMetadataException())

            return Outcome.Ok(KMPFileMetadata(size = size as Long))
        } catch (e: Exception) {
            return Outcome.Error(e)
        }
    }

    override suspend fun exists(ref: KMPFileRef): Boolean {
        return try {
            val url = ref.toNSURL()
            NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")
        } catch (e: Exception) {
            false
        }
    }
}

private class IOSPickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {

    val resultFlow = MutableSharedFlow<NSURL?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
        coroutineScope.launch {
            resultFlow.emit(didPickDocumentAtURL)
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        coroutineScope.launch {
            resultFlow.emit(null)
        }
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        coroutineScope.launch {
            resultFlow.emit(didPickDocumentsAtURLs.firstOrNull() as NSURL)
        }
    }
}

private class IOSDocumentBrowserDelegate : NSObject(), UIDocumentBrowserViewControllerDelegateProtocol {

    override fun documentBrowser(
        controller: UIDocumentBrowserViewController,
        didRequestDocumentCreationWithHandler: (NSURL?, UIDocumentBrowserImportMode) -> Unit
    ) {
        val url = NSURL(fileURLWithPath = "${NSFileManager.defaultManager.temporaryDirectory.path}/test.tmp")
        NSFileManager.defaultManager.createFileAtPath(url.path ?: "", null, null)

        didRequestDocumentCreationWithHandler(
            url,
            UIDocumentBrowserImportMode.UIDocumentBrowserImportModeMove
        )
    }

    override fun documentBrowser(
        controller: UIDocumentBrowserViewController,
        didImportDocumentAtURL: NSURL,
        toDestinationURL: NSURL
    ) {
        println("Imported")
    }

    override fun documentBrowser(
        controller: UIDocumentBrowserViewController,
        failedToImportDocumentAtURL: NSURL,
        error: NSError?
    ) {
        println("Failed to import")
    }

// //    @Suppress("CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
//    override fun documentBrowser(controller: UIDocumentBrowserViewController, didPickDocumentsAtURLs: List<*>) {
//        println("picked")
//    }
}

actual fun KMPFileRef.source(): Outcome<Source, Exception> {
    return try {
        val url = toNSURL()
        Outcome.Ok(NSInputStream(uRL = url).source())
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

actual fun KMPFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return try {
        val url = toNSURL()
        Outcome.Ok(NSOutputStream(uRL = url, mode == KMPFileWriteMode.Append).sink())
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.toKMPFileRef(): KMPFileRef {
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

    return KMPFileRef(
        ref = ref ?: "",
        name = path?.split("/")?.lastOrNull() ?: "",
        isDirectory = hasDirectoryPath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun KMPFileRef.toNSURL(): NSURL {
    val data = NSMutableData()
    ref.decodeBase64Bytes().usePinned {
        data.appendBytes(it.addressOf(0).reinterpret(), it.get().size.convert())
    }

    return NSURL(
        byResolvingBookmarkData = data,
        options = NSURLBookmarkCreationMinimalBookmark,
        relativeToURL = null,
        bookmarkDataIsStale = null,
        error = null,
    )
}
