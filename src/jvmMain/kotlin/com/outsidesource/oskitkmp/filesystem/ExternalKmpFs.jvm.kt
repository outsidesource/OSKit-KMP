package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.filesystem.ObjCJna.cls
import com.outsidesource.oskitkmp.filesystem.ObjCJna.invokeLong
import com.outsidesource.oskitkmp.filesystem.ObjCJna.invokePtr
import com.outsidesource.oskitkmp.filesystem.ObjCJna.invokeVoid
import com.outsidesource.oskitkmp.filesystem.ObjCJna.nsStringArrayFromUtf8Array
import com.outsidesource.oskitkmp.filesystem.ObjCJna.nsStringFromUtf8
import com.outsidesource.oskitkmp.filesystem.ObjCJna.nsUrlFileURLFromPath
import com.outsidesource.oskitkmp.filesystem.ObjCJna.runOnMainThread
import com.outsidesource.oskitkmp.filesystem.ObjCJna.sel
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Callback
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
    private val NSOpenPanel = cls("NSOpenPanel")
    private val NSSavePanel = cls("NSSavePanel")

    private val URLs = sel("URLs")
    private val URL = sel("URL")
    private val openPanel = sel("openPanel")
    private val savePanel = sel("savePanel")
    private val runModal = sel("runModal")
    private val setAllowsMultipleSelection = sel("setAllowsMultipleSelection:")
    private val setCanChooseDirectories = sel("setCanChooseDirectories:")
    private val setCanChooseFiles = sel("setCanChooseFiles:")
    private val setAllowedFileTypes = sel("setAllowedFileTypes:")
    private val setDirectoryURL = sel("setDirectoryURL:")
    private val count = sel("count")
    private val objectAtIndex = sel("objectAtIndex:")
    private val path = sel("path")
    private val UTF8String = sel("UTF8String")
    private val setMessage = sel("setMessage:")
    private val setNameFieldStringValue = sel("setNameFieldStringValue:")

    fun openFile(
        start: KmpFsRef? = null,
        filters: KmpFileFilter? = emptyList(),
        allowMultiple: Boolean = true,
        title: String? = null,
    ): List<KmpFsRef> = ObjCJna.withAutoReleasePool {
        runOnMainThread {
            val panel = NSOpenPanel.invokePtr(openPanel)!!
            panel.invokeVoid(setAllowsMultipleSelection, if (allowMultiple) 1 else 0)
            panel.invokeVoid(setCanChooseFiles, 1)

            setFilters(panel, filters)
            setStartDir(panel, start)
            setTitle(panel, title)
            val result = panel.invokeLong(runModal)
            if (result != 1L) emptyList() else collectURLs(panel, multiple = allowMultiple)
        }
    }

    fun openDirectoryDialog(
        start: KmpFsRef? = null,
        allowMultiple: Boolean = false,
        title: String? = null,
    ): List<KmpFsRef> = ObjCJna.withAutoReleasePool {
        val panel = NSOpenPanel.invokePtr(openPanel)!!

        panel.invokeVoid(setAllowsMultipleSelection, if (allowMultiple) 1 else 0)
        panel.invokeVoid(setCanChooseDirectories, 1)
        panel.invokeVoid(setCanChooseFiles, 0)

        setStartDir(panel, start)
        setTitle(panel, title)

        if (panel.invokeLong(runModal) != 1L) emptyList() else collectURLs(panel, multiple = allowMultiple)
    }

    fun saveDialog(
        start: KmpFsRef? = null,
        suggestedName: String? = null,
        filters: KmpFileFilter = emptyList(), // restrict save types (adds extension popup)
        title: String? = null,
    ): KmpFsRef? = ObjCJna.withAutoReleasePool {
        val panel = NSSavePanel.invokePtr(savePanel)!!

        setFilters(panel, filters)
        setStartDir(panel, start)
        setTitle(panel, title)
        if (!suggestedName.isNullOrBlank()) {
            panel.invokeVoid(setNameFieldStringValue, nsStringFromUtf8(suggestedName))
        }

        if (panel.invokeLong(runModal) != 1L) null else collectSingleURL(panel)
    }

    private fun setFilters(panel: Pointer, filters: KmpFileFilter?) {
        val normalized = filters?.map { it.extension } ?: emptyList()
        if (normalized.isNotEmpty()) panel.invokeVoid(setAllowedFileTypes, nsStringArrayFromUtf8Array(normalized))
    }

    private fun setStartDir(panel: Pointer, start: KmpFsRef?) {
        if (start == null) return
        panel.invokeVoid(setDirectoryURL, nsUrlFileURLFromPath(start.ref))
    }

    private fun setTitle(panel: Pointer, title: String?) {
        if (!title.isNullOrBlank()) panel.invokeVoid(setMessage, nsStringFromUtf8(title))
    }

    private fun collectURLs(panel: Pointer, multiple: Boolean): List<KmpFsRef> {
        return if (multiple) {
            val urls = panel.invokePtr(URLs)!!
            val count = urls.invokeLong(count).toInt()
            (0 until count).mapNotNull { i ->
                val url = urls.invokePtr(objectAtIndex, i) ?: return@mapNotNull null
                urlToUri(url)
            }
        } else {
            collectSingleURL(panel)?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun collectSingleURL(panel: Pointer): KmpFsRef? {
        val url = panel.invokePtr(URL) ?: return null
        return urlToUri(url)
    }

    // TODO: Clean up
    private fun urlToUri(nsUrl: Pointer): KmpFsRef? {
        val nsPath = nsUrl.invokePtr(path) ?: return null
        val cStr = nsPath.invokePtr(UTF8String) ?: return null
        val path = cStr.getString(0, StandardCharsets.UTF_8.name())
        return KmpFsRef(
            ref = Paths.get(path).toUri().toString(),
            isDirectory = false,
            name = "",
            fsType = KmpFsType.External,
        )
    }
}

object ObjCJna {
    private val oskitRunnerTasks = ConcurrentHashMap<Long, Runnable>()
    private val objc = NativeLibrary.getInstance("objc")

    private val objc_msgSend = func("objc_msgSend")
    private val class_addMethod = func("class_addMethod")
    private val objc_getClass = func("objc_getClass")
    private val sel_registerName = func("sel_registerName")
    private val objc_allocateClassPair = func("objc_allocateClassPair")
    private val objc_registerClassPair = func("objc_registerClassPair")

    private val NSObject = cls("NSObject")
    private val NSAutoreleasePool = cls("NSAutoreleasePool")
    private val NSString = cls("NSString")
    private val NSArray = cls("NSArray")
    private val NSURL = cls("NSURL")
    private val NSNumber = cls("NSNumber")
    private val OSKitRunner: Pointer by lazy {
        val name = "OSKitRunner_${System.identityHashCode(this)}"
        val runnerClass = objc_allocateClassPair.invokePointer(arrayOf(NSObject, name, 0))!!
        val callback = ObjcJNACallback { self, _, _ ->
            try {
                oskitRunnerTasks.remove(Pointer.nativeValue(self))?.run()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        class_addMethod.invokeInt(arrayOf(runnerClass, run, callback, "v@:@"))
        objc_registerClassPair.invokeVoid(arrayOf(runnerClass))
        runnerClass
    }

    private val performOnMain = sel("performSelectorOnMainThread:withObject:waitUntilDone:")
    private val new = sel("new")
    private val run = sel("run:")
    private val alloc = sel("alloc")
    private val init = sel("init")
    private val drain = sel("drain")
    private val stringWithUTF8String = sel("stringWithUTF8String:")
    private val arrayWithObjects_count = sel("arrayWithObjects:count:")
    private val fileURLWithPath = sel("fileURLWithPath:")
    private val numberWithInt = sel("numberWithInt:")
    private val numberWithLong = sel("numberWithLong:")
    private val intValue = sel("intValue")
    private val longValue = sel("longValue")

    private fun interface ObjcJNACallback : Callback {
        fun invoke(self: Pointer?, cmd: Pointer?, arg: Pointer?)
    }

    fun <T> runOnMainThread(block: () -> T): T {
        val runner = OSKitRunner.invokePtr(new)!!
        var result: T? = null
        oskitRunnerTasks[Pointer.nativeValue(runner)] = Runnable { result = block() }
        runner.invokeVoid(performOnMain, run, Pointer.NULL, 1)
        return result!!
    }

    fun <T> withAutoReleasePool(block: () -> T): T {
        val pool = NSAutoreleasePool.invokePtr(alloc)!!.invokePtr(init)!!
        return try {
            block()
        } finally {
            pool.invokeVoid(drain)
        }
    }

    fun func(name: String): Function = objc.getFunction(name)
    fun sel(name: String): Pointer = sel_registerName.invokePointer(arrayOf(name))!!
    fun cls(name: String): Pointer = objc_getClass.invokePointer(arrayOf(name))!!

    fun Pointer.invokePtr(sel: Pointer, vararg args: Any?): Pointer? =
        objc_msgSend.invokePointer(arrayOf(this, sel, *args))
    fun Pointer.invokeLong(sel: Pointer, vararg args: Any?): Long =
        objc_msgSend.invokeLong(arrayOf(this, sel, *args))
    fun Pointer.invokeVoid(sel: Pointer, vararg args: Any?): Unit =
        objc_msgSend.invokeVoid(arrayOf(this, sel, *args))

    fun nsStringFromUtf8(s: String): Pointer {
        val bytes = Native.toByteArray(s, StandardCharsets.UTF_8.name())
        return NSString.invokePtr(stringWithUTF8String, bytes)!!
    }

    fun nsStringArrayFromUtf8Array(items: List<String>): Pointer {
        val nsStrings = items.map { nsStringFromUtf8(it) }.toTypedArray()
        return NSArray.invokePtr(arrayWithObjects_count, nsStrings, nsStrings.size)!!
    }

    fun nsUrlFileURLFromPath(path: String): Pointer {
        val nsPath = nsStringFromUtf8(path)
        return NSURL.invokePtr(fileURLWithPath, nsPath)!!
    }

    fun nsArrayOf(vararg obj: Pointer): Pointer = NSArray.invokePtr(arrayWithObjects_count, obj, obj.size)!!
    fun nsNumberFromInt(value: Int) = NSNumber.invokePtr(numberWithInt, value)
    fun nsNumberFromLong(value: Long) = NSNumber.invokePtr(numberWithLong, value)
    fun nsNumberToInt(value: Pointer): Int = value.invokeLong(intValue).toInt()
    fun nsNumberToLong(value: Pointer): Long = value.invokeLong(longValue)
}
