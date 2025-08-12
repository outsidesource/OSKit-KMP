package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.filesystem.ObjCJNA.cls
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.nsStringArrayFromUtf8Array
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.nsStringFromUtf8
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.nsUrlFileURLWithPath
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.runLong
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.runOnMainThread
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.runPtr
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.runVoid
import com.outsidesource.oskitkmp.filesystem.ObjCJNA.sel
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import okio.FileSystem
import okio.Path.Companion.toPath
import java.awt.FileDialog
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

actual fun platformExternalKmpFs(): IExternalKmpFs = JvmExternalKmpFs()

internal class JvmExternalKmpFs : IExternalKmpFs, IInitializableKmpFs {
    private val fsMixin = NonJsKmpFsMixin(fsType = KmpFsType.External, isInitialized = { context != null })
    private var context: KmpFsContext? = null

    override fun init(fileHandlerContext: KmpFsContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
//            if (Platform.current == Platform.Linux) return nativeOpenFilePicker(startingDir, filter)

//            FileKit.init("Hello!")
//            val file = FileKit.openFilePicker()
//            println("Hello $file")

            val test = MacFilePicker.openFile(startingDir, filter, allowMultiple = false)
            println(test)
            return Outcome.Error(KmpFsError.Unknown(Unit))

//            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
//            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)
//            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
//            dialog.directory = startingDir?.ref?.toPath()?.pathString
//            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
//            dialog.isVisible = true
//
//            if (dialog.file == null) return Outcome.Ok(null)
//
//            val ref = KmpFsRef(
//                ref = joinPathSegments(dialog.directory, dialog.file),
//                name = dialog.file,
//                isDirectory = false,
//                fsType = KmpFsType.External,
//            )
//
//            Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
//            if (Platform.current == Platform.Linux) return nativeOpenFilesPicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.isMultipleMode = true
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.files == null || dialog.files.isEmpty()) return Outcome.Ok(null)

            val refs = dialog.files.map { file ->
                KmpFsRef(
                    ref = joinPathSegments(dialog.directory, file.name),
                    name = file.name,
                    isDirectory = false,
                    fsType = KmpFsType.External,
                )
            }

            Outcome.Ok(refs)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            // Use TinyFileDialogs because there is no AWT directory picker
//            val directory = TinyFileDialogs.tinyfd_selectFolderDialog("Select Folder", startingDir?.ref ?: "")
//                ?: return Outcome.Ok(null)
//            val ref = KmpFsRef(
//                ref = directory,
//                name = directory.toPath().name,
//                isDirectory = true,
//                fsType = KmpFsType.External,
//            )

//            Outcome.Ok(ref)
            Outcome.Ok(null)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
//            if (Platform.current == Platform.Linux) return nativeSaveFilePicker(fileName, startingDir)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)
            val dialog = FileDialog(context.window, "Save File", FileDialog.SAVE)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.file = fileName
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KmpFsRef(
                ref = joinPathSegments(dialog.directory, dialog.file),
                name = dialog.file,
                isDirectory = false,
                fsType = KmpFsType.External,
            )

            FileSystem.SYSTEM.sink(ref.ref.toPath(), mustCreate = true)

            Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun saveFile(
        bytes: ByteArray,
        fileName: String,
    ): Outcome<Unit, KmpFsError> = nonJsSaveFile(bytes, fileName)

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)

        return try {
            val localPath = path.toPath()
            val exists = FileSystem.SYSTEM.exists(localPath)

            if (!exists) return Outcome.Error(KmpFsError.RefNotFound)
            val metadata = FileSystem.SYSTEM.metadata(localPath)

            val ref = KmpFsRef(
                ref = localPath.pathString,
                name = localPath.name,
                isDirectory = metadata.isDirectory,
                fsType = KmpFsType.External,
            )
            return Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

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

object MacFilePicker {
    private val _NSOpenPanel = cls("NSOpenPanel")
    private val _NSSavePanel = cls("NSSavePanel")

    private val _URLs = sel("URLs")
    private val _URL = sel("URL")
    private val _openPanel = sel("openPanel")
    private val _savePanel = sel("savePanel")
    private val _runModal = sel("runModal")
    private val _setAllowsMultipleSelection = sel("setAllowsMultipleSelection:")
    private val _setCanChooseDirectories = sel("setCanChooseDirectories:")
    private val _setCanChooseFiles = sel("setCanChooseFiles:")
    private val _setAllowedFileTypes = sel("setAllowedFileTypes:")
    private val _setDirectoryURL = sel("setDirectoryURL:")
    private val _count = sel("count")
    private val _objectAtIndex = sel("objectAtIndex:")
    private val _path = sel("path")
    private val _UTF8String = sel("UTF8String")
    private val _setMessage = sel("setMessage:")
    private val _setNameFieldStringValue = sel("setNameFieldStringValue:")

    fun openFile(
        start: KmpFsRef? = null,
        filters: KmpFileFilter? = emptyList(),
        allowMultiple: Boolean = true,
        title: String? = null,
    ): List<KmpFsRef> = ObjCJNA.withAutoReleasePool {
        runOnMainThread {
            val panel = runPtr(_NSOpenPanel, _openPanel)!!
            runVoid(panel, _setAllowsMultipleSelection, if (allowMultiple) 1 else 0)
            runVoid(panel, _setCanChooseFiles, 1)

            setFilters(panel, filters)
            setStartDir(panel, start)
            setTitle(panel, title)
            val result = runLong(panel, _runModal)
            if (result != 1L) emptyList() else collectURLs(panel, multiple = allowMultiple)
        }
    }

    fun openDirectoryDialog(
        start: KmpFsRef? = null,
        allowMultiple: Boolean = false,
        title: String? = null,
    ): List<KmpFsRef> = ObjCJNA.withAutoReleasePool {
        val panel = runPtr(_NSOpenPanel, _openPanel)!!

        runVoid(panel, _setAllowsMultipleSelection, if (allowMultiple) 1 else 0)
        runVoid(panel, _setCanChooseDirectories, 1)
        runVoid(panel, _setCanChooseFiles, 0)

        setStartDir(panel, start)
        setTitle(panel, title)

        if (runLong(panel, _runModal) != 1L) emptyList() else collectURLs(panel, multiple = allowMultiple)
    }

    fun saveDialog(
        start: KmpFsRef? = null,
        suggestedName: String? = null,
        filters: KmpFileFilter = emptyList(), // restrict save types (adds extension popup)
        title: String? = null,
    ): KmpFsRef? = ObjCJNA.withAutoReleasePool {
        val panel = runPtr(_NSSavePanel, _savePanel)!!

        setFilters(panel, filters)
        setStartDir(panel, start)
        setTitle(panel, title)
        if (!suggestedName.isNullOrBlank()) {
            runVoid(panel, _setNameFieldStringValue, nsStringFromUtf8(suggestedName))
        }

        if (runLong(panel, _runModal) != 1L) null else collectSingleURL(panel)
    }

    private fun setFilters(panel: Pointer, filters: KmpFileFilter?) {
        val normalized = filters?.map { it.extension } ?: emptyList()
        if (normalized.isNotEmpty()) runVoid(panel, _setAllowedFileTypes, nsStringArrayFromUtf8Array(normalized))
    }

    private fun setStartDir(panel: Pointer, start: KmpFsRef?) {
        if (start == null) return
        runVoid(panel, _setDirectoryURL, nsUrlFileURLWithPath(start.ref))
    }

    private fun setTitle(panel: Pointer, title: String?) {
        if (!title.isNullOrBlank()) runVoid(panel, _setMessage, nsStringFromUtf8(title))
    }

    private fun collectURLs(panel: Pointer, multiple: Boolean): List<KmpFsRef> {
        return if (multiple) {
            val urls = runPtr(panel, _URLs)!!
            val count = runLong(urls, _count).toInt()
            (0 until count).mapNotNull { i ->
                val url = runPtr(urls, _objectAtIndex, i) ?: return@mapNotNull null
                urlToUri(url)
            }
        } else {
            collectSingleURL(panel)?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun collectSingleURL(panel: Pointer): KmpFsRef? {
        val url = runPtr(panel, _URL) ?: return null
        return urlToUri(url)
    }

    // TODO: Clean up
    private fun urlToUri(nsUrl: Pointer): KmpFsRef? {
        val nsPath = runPtr(nsUrl, _path) ?: return null
        val cStr = runPtr(nsPath, _UTF8String) ?: return null
        val path = cStr.getString(0, StandardCharsets.UTF_8.name())
        return KmpFsRef(
            ref = Paths.get(path).toUri().toString(),
            isDirectory = false,
            name = "",
            fsType = KmpFsType.External,
        )
    }
}

object ObjCJNA {
    private val oskitRunnerTasks = ConcurrentHashMap<Long, Runnable>()
    private val objc = NativeLibrary.getInstance("objc")

    private val objc_msgSend = func("objc_msgSend")
    private val class_addMethod = func("class_addMethod")
    private val objc_getClass = func("objc_getClass")
    private val sel_registerName = func("sel_registerName")
    private val objc_allocateClassPair = func("objc_allocateClassPair")
    private val objc_registerClassPair = func("objc_registerClassPair")

    private val _NSObject = cls("NSObject")
    private val _NSAutoreleasePool = cls("NSAutoreleasePool")
    private val _NSString = cls("NSString")
    private val _NSArray = cls("NSArray")
    private val _NSURL = cls("NSURL")
    private val _OSKitRunner: Pointer by lazy {
        val name = "OSKitRunner_${System.identityHashCode(this)}"
        val runnerClass = objc_allocateClassPair.invokePointer(arrayOf(_NSObject, name, 0))!!
        val callback = ObjcJNACallback { self: Pointer? ->
            try {
                oskitRunnerTasks.remove(Pointer.nativeValue(self))?.run()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        class_addMethod.invokeInt(arrayOf(runnerClass, _run, callback, "v"))
        objc_registerClassPair.invokeVoid(arrayOf(runnerClass))
        runnerClass
    }

    private val _performOnMain = sel("performSelectorOnMainThread:withObject:waitUntilDone:")
    private val _new = sel("new")
    private val _run = sel("run:")
    private val _alloc = sel("alloc")
    private val _init = sel("init")
    private val _drain = sel("drain")
    private val _stringWithUTF8String = sel("stringWithUTF8String:")
    private val _arrayWithObjects_count = sel("arrayWithObjects:count:")
    private val _fileURLWithPath = sel("fileURLWithPath:")

    private fun interface ObjcJNACallback : Callback {
        fun invoke(self: Pointer?)
    }

    // TODO: Should this use an auto-release pool?
    fun <T> runOnMainThread(block: () -> T): T {
        val runner = runPtr(_OSKitRunner, _new)!!
        var result: T? = null
        oskitRunnerTasks[Pointer.nativeValue(runner)] = Runnable { result = block() }
        runVoid(runner, _performOnMain, _run, Pointer.NULL, 1)
        return result!!
    }

    fun <T> withAutoReleasePool(block: () -> T): T {
        val pool = runPtr(runPtr(_NSAutoreleasePool, _alloc)!!, _init)!!
        return try {
            block()
        } finally {
            runVoid(pool, _drain)
        }
    }

    fun func(name: String): Function = objc.getFunction(name)
    fun sel(name: String): Pointer = sel_registerName.invokePointer(arrayOf(name))!!
    fun cls(name: String): Pointer = objc_getClass.invokePointer(arrayOf(name))!!

    fun runPtr(rcv: Pointer, sel: Pointer, vararg args: Any?): Pointer? =
        objc_msgSend.invokePointer(arrayOf(rcv, sel, *args))
    fun runLong(rcv: Pointer, sel: Pointer, vararg args: Any?): Long =
        objc_msgSend.invokeLong(arrayOf(rcv, sel, *args))
    fun runVoid(rcv: Pointer, sel: Pointer, vararg args: Any?): Unit =
        objc_msgSend.invokeVoid(arrayOf(rcv, sel, *args))

    fun nsStringFromUtf8(s: String): Pointer {
        val bytes = Native.toByteArray(s, StandardCharsets.UTF_8.name())
        return runPtr(_NSString, _stringWithUTF8String, bytes)!!
    }

    fun nsStringArrayFromUtf8Array(items: List<String>): Pointer {
        val nsStrings = items.map { nsStringFromUtf8(it) }.toTypedArray()
        return runPtr(_NSArray, _arrayWithObjects_count, nsStrings, nsStrings.size)!!
    }

    fun nsUrlFileURLWithPath(path: String): Pointer {
        val nsPath = nsStringFromUtf8(path)
        return runPtr(_NSURL, _fileURLWithPath, nsPath)!!
    }
}
