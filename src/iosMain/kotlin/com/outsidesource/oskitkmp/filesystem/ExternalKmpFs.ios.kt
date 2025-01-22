package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.Deferrer
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

actual fun platformExternalKmpFs(): IExternalKmpFs = IosExternalKmpFs()

internal class IosExternalKmpFs : IExternalKmpFs, IInitializableKmpFs {
    private var context: KmpFsContext? = null

    private val documentPickerDelegate = IOSPickerDelegate()

    private val directoryPickerDelegate = IOSPickerDelegate()
    private val directoryPickerViewController = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeFolder),
    ).apply {
        delegate = directoryPickerDelegate
        allowsMultipleSelection = false
    }

    override fun init(fileHandlerContext: KmpFsContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        try {
            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)

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
            return Outcome.Ok(url.toKmpFileRef(isDirectory = false))
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        try {
            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)

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
            val refs = urls.map { it.toKmpFileRef(false) }

            return Outcome.Ok(refs)
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        val directory = pickDirectory(startingDir).unwrapOrReturn { return it } ?: return Outcome.Ok(null)
        return resolveFile(directory, fileName, create = true)
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        try {
            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)

            withContext(Dispatchers.Main) {
                directoryPickerViewController.directoryURL = startingDir?.toNSURL()
                context.rootController.presentViewController(
                    viewControllerToPresent = directoryPickerViewController,
                    animated = true,
                    completion = null,
                )
            }

            val url = directoryPickerDelegate.resultFlow.firstOrNull()?.firstOrNull() ?: return Outcome.Ok(null)
            return Outcome.Ok(url.toKmpFileRef(isDirectory = true))
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        val deferrer = Deferrer()

        return try {
            val directoryUrl = dir.toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)
            directoryUrl.startAccessingSecurityScopedResource()
            deferrer.defer { directoryUrl.stopAccessingSecurityScopedResource() }

            val url = directoryUrl.URLByAppendingPathComponent(fileName) ?: return Outcome.Error(KmpFsError.InvalidRef)
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists && !create) return Outcome.Error(KmpFsError.RefNotFound)
            if (exists && url.hasDirectoryPath()) return Outcome.Error(KmpFsError.RefExistsAsDirectory)
            if (!exists && create) {
                val created = NSFileManager.defaultManager.createFileAtPath(
                    path = url.path ?: "",
                    contents = null,
                    attributes = null,
                )
                if (!created) return Outcome.Error(KmpFsError.RefNotCreated)
            }

            Outcome.Ok(url.toKmpFileRef(isDirectory = false))
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        } finally {
            deferrer.run()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        val deferrer = Deferrer()

        return try {
            val directoryUrl = dir.toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)
            directoryUrl.startAccessingSecurityScopedResource()
            deferrer.defer { directoryUrl.stopAccessingSecurityScopedResource() }

            val url = directoryUrl.URLByAppendingPathComponent(name) ?: return Outcome.Error(KmpFsError.InvalidRef)
            val exists = NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")

            if (!exists && !create) return Outcome.Error(KmpFsError.RefNotFound)
            if (exists && !url.hasDirectoryPath()) return Outcome.Error(KmpFsError.RefExistsAsFile)
            if (!exists && create) {
                val created = NSFileManager.defaultManager.createDirectoryAtPath(
                    path = url.path ?: "",
                    withIntermediateDirectories = false,
                    attributes = null,
                    error = null,
                )
                if (!created) return Outcome.Error(KmpFsError.RefNotCreated)
            }

            Outcome.Ok(url.toKmpFileRef(isDirectory = true))
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        } finally {
            deferrer.run()
        }
    }

    override suspend fun saveFile(
        bytes: ByteArray,
        fileName: String,
    ): Outcome<Unit, KmpFsError> = nonJsSaveFile(bytes, fileName)

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        return Outcome.Error(KmpFsError.NotSupported)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        val deferrer = Deferrer()

        return try {
            val url = ref.toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)
            url.startAccessingSecurityScopedResource()
            deferrer.defer { url.stopAccessingSecurityScopedResource() }

            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = NSFileManager.defaultManager.removeItemAtPath(url.path ?: "", error.ptr)
                return if (success) Outcome.Ok(Unit) else Outcome.Error(KmpFsError.Unknown(error.value ?: Unit))
            }
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        } finally {
            deferrer.run()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> {
        val deferrer = Deferrer()

        return try {
            val list = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val directoryUrl = dir.toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)

                directoryUrl.startAccessingSecurityScopedResource()
                deferrer.defer { directoryUrl.stopAccessingSecurityScopedResource() }

                val paths = NSFileManager.defaultManager.contentsOfDirectoryAtPath(directoryUrl.path ?: "", error.ptr)
                    ?: return Outcome.Error(KmpFsError.Unknown(Unit))

                if (!isRecursive) {
                    val list = paths.mapNotNull {
                        val path = it as? String ?: return@mapNotNull null
                        directoryUrl.URLByAppendingPathComponent(path)?.toKmpFileRef(isDirectory = false)
                    }
                    return Outcome.Ok(list)
                }

                paths.flatMap {
                    val path = it as? String ?: return@flatMap emptyList()
                    val url = directoryUrl.URLByAppendingPathComponent(path) ?: return@flatMap emptyList()

                    buildList {
                        val file = url.toKmpFileRef(isDirectory = false)
                        add(file)

                        if (url.hasDirectoryPath) {
                            addAll(list(file).unwrapOrNull() ?: emptyList())
                        }
                    }
                }
            }

            Outcome.Ok(list)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        } finally {
            deferrer.run()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> {
        val deferrer = Deferrer()

        try {
            val attributes = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val url = ref.toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)

                url.startAccessingSecurityScopedResource()
                deferrer.defer { url.stopAccessingSecurityScopedResource() }

                val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(url.path ?: "", error.ptr) ?: run {
                    url.stopAccessingSecurityScopedResource()
                    return Outcome.Error(KmpFsError.Unknown(Unit))
                }
                attrs
            }

            val size = attributes[NSFileSize] ?: return Outcome.Error(KmpFsError.Unknown(Unit))

            return Outcome.Ok(KmpFileMetadata(size = size as Long))
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        } finally {
            deferrer.run()
        }
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        val deferrer = Deferrer()

        return try {
            val url = ref.toNSURL() ?: return false

            url.startAccessingSecurityScopedResource()
            deferrer.defer { url.stopAccessingSecurityScopedResource() }

            NSFileManager.defaultManager.fileExistsAtPath(url.path ?: "")
        } catch (t: Throwable) {
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
