package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
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
        val context = context ?: return Outcome.Error(NotInitializedException())

        withContext(Dispatchers.Main) {
            context.rootController.presentViewController(
                viewControllerToPresent = documentPickerViewController,
                animated = true,
                completion = null
            )
        }

        val url = documentPickerDelegate.resultFlow.firstOrNull() ?: return Outcome.Ok(null)
        val ref = KMPFileRef(
            ref = url.path ?: "",
            name = url.path?.split("/")?.lastOrNull() ?: "",
            isDirectory = false
        )

        return Outcome.Ok(ref)
    }

    override suspend fun pickSaveFile(defaultName: String?): Outcome<KMPFileRef?, Exception> {
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
    }

    override suspend fun pickFolder(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        val context = context ?: return Outcome.Error(NotInitializedException())

        withContext(Dispatchers.Main) {
            context.rootController.presentViewController(
                viewControllerToPresent = directoryPickerViewController,
                animated = true,
                completion = null
            )
        }

        try {
            val url = directoryPickerDelegate.resultFlow.firstOrNull() ?: return Outcome.Ok(null)
            val ref = KMPFileRef(
                ref = url.path ?: "",
                name = url.path?.split("/")?.lastOrNull() ?: return Outcome.Error(FileOpenException()),
                isDirectory = true
            )

            return Outcome.Ok(ref)
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
            val url = NSURL(fileURLWithPath = "${dir.ref}/$name")
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

            val ref = KMPFileRef(
                ref = url.path ?: "",
                name = name,
                isDirectory = false,
            )

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun rename(ref: KMPFileRef, name: String): Outcome<KMPFileRef, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun list(dir: KMPFileRef, isRecursive: Boolean): Outcome<List<KMPFileRef>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(ref: KMPFileRef): Boolean {
        TODO("Not yet implemented")
    }
}

actual fun KMPFileRef.source(): Outcome<Source, Exception> {
    return try {
        Outcome.Ok(NSInputStream(uRL = NSURL(fileURLWithPath = ref)).source())
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

actual fun KMPFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return try {
        Outcome.Ok(NSOutputStream(uRL = NSURL(fileURLWithPath = ref), mode == KMPFileWriteMode.Append).sink())
    } catch (e: Exception) {
        Outcome.Error(e)
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
        println("Document Creation")
        val path = NSURL(fileURLWithPath = "${NSFileManager.defaultManager.temporaryDirectory}/test.tmp")
        val file = UIDocument(fileURL = path)
        file.saveToURL(path, UIDocumentSaveOperation.UIDocumentSaveForCreating) {
            println("Created")
            didRequestDocumentCreationWithHandler(
                file.fileURL,
                UIDocumentBrowserImportMode.UIDocumentBrowserImportModeMove
            )
        }
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
}
