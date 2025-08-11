package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import com.sun.jna.Callback
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

            CocoaMain.executeOnMainThread {
                val test = MacFilePicker.openDialog(
                    null,
                    listOf(),
                    allowMultiple = false,
                    allowDirectories = false,
                    title = "Testing 123!",
                )
                println(test)
            }

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(KmpFsError.NotInitialized)
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KmpFsRef(
                ref = joinPathSegments(dialog.directory, dialog.file),
                name = dialog.file,
                isDirectory = false,
                fsType = KmpFsType.External,
            )

            Outcome.Ok(ref)
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

/**
 * macOS native file dialogs via NSOpenPanel/NSSavePanel using JNA (no JNI/C).
 * Works with arbitrary extensions (e.g., ".abc") without UTI registration.
 */
object MacFilePicker {

    // ---- Native libs (load once) ----
    private val OBJC = NativeLibraryHolder("objc")
    private val APPKIT = NativeLibraryHolder("AppKit")

    // ---- Objective‑C runtime ----
    private val objc_msgSend = OBJC.func("objc_msgSend")
    private val objc_getClass = OBJC.func("objc_getClass")
    private val sel_registerName = OBJC.func("sel_registerName")

    // ---- Selectors / classes (cached) ----
    private val SEL_alloc = sel("alloc")
    private val SEL_init = sel("init")
    private val SEL_drain = sel("drain")

    private val CLS_NSAutoreleasePool = cls("NSAutoreleasePool")
    private val CLS_NSOpenPanel = cls("NSOpenPanel")
    private val CLS_NSSavePanel = cls("NSSavePanel")
    private val CLS_NSString = cls("NSString")
    private val CLS_NSURL = cls("NSURL")

    private val SEL_openPanel = sel("openPanel")
    private val SEL_savePanel = sel("savePanel")
    private val SEL_runModal = sel("runModal")

    private val SEL_setAllowsMultipleSelection = sel("setAllowsMultipleSelection:")
    private val SEL_setCanChooseDirectories = sel("setCanChooseDirectories:")
    private val SEL_setCanChooseFiles = sel("setCanChooseFiles:")
    private val SEL_setAllowedFileTypes = sel("setAllowedFileTypes:")
    private val SEL_setDirectoryURL = sel("setDirectoryURL:")
    private val SEL_URLs = sel("URLs")
    private val SEL_URL = sel("URL")
    private val SEL_count = sel("count")
    private val SEL_objectAtIndex = sel("objectAtIndex:")
    private val SEL_path = sel("path")
    private val SEL_UTF8String = sel("UTF8String")
    private val SEL_setTitle = sel("setTitle:")
    private val SEL_setMessage = sel("setMessage:")
    private val SEL_setNameFieldStringValue = sel("setNameFieldStringValue:")

    private val SEL_stringWithUTF8String = sel("stringWithUTF8String:")
    private val SEL_fileURLWithPath = sel("fileURLWithPath:")
    private val CLS_NSArray = cls("NSArray")
    private val SEL_arrayWithObjects_count = sel("arrayWithObjects:count:")

    // ---- Public API ----

    /**
     * Open dialog for files (optionally directories too).
     */
    fun openDialog(
        start: KmpFsRef? = null,
        filters: KmpFileFilter = emptyList(), // ["abc", ".png"]
        allowMultiple: Boolean = true,
        allowDirectories: Boolean = false,
        title: String? = null,
        message: String? = null,
    ): List<KmpFsRef> = withPool {
        val panel = msgPtr(CLS_NSOpenPanel, SEL_openPanel)!!

        msgVoid(panel, SEL_setAllowsMultipleSelection, if (allowMultiple) 1 else 0)
        msgVoid(panel, SEL_setCanChooseDirectories, if (allowDirectories) 1 else 0)
        msgVoid(panel, SEL_setCanChooseFiles, 1) // keep files enabled by default

        setFilters(panel, filters)
        setStartDir(panel, start)
        setTitleAndMessage(panel, title, message)

        if (msgLong(panel, SEL_runModal) != 1L) emptyList() else collectURLs(panel, multiple = allowMultiple)
    }

    /**
     * Open dialog for directories only.
     */
    fun openDirectoryDialog(
        start: KmpFsRef? = null,
        allowMultiple: Boolean = false,
        title: String? = null,
        message: String? = null,
    ): List<KmpFsRef> = withPool {
        val panel = msgPtr(CLS_NSOpenPanel, SEL_openPanel)!!

        msgVoid(panel, SEL_setAllowsMultipleSelection, if (allowMultiple) 1 else 0)
        msgVoid(panel, SEL_setCanChooseDirectories, 1)
        msgVoid(panel, SEL_setCanChooseFiles, 0)

        setStartDir(panel, start)
        setTitleAndMessage(panel, title, message)

        if (msgLong(panel, SEL_runModal) != 1L) emptyList() else collectURLs(panel, multiple = allowMultiple)
    }

    /**
     * Save dialog (returns chosen target URI or null if cancelled).
     */
    fun saveDialog(
        start: KmpFsRef? = null,
        suggestedName: String? = null,
        filters: KmpFileFilter = emptyList(), // restrict save types (adds extension popup)
        title: String? = null,
        message: String? = null,
    ): KmpFsRef? = withPool {
        val panel = msgPtr(CLS_NSSavePanel, SEL_savePanel)!!

        setFilters(panel, filters)
        setStartDir(panel, start)
        setTitleAndMessage(panel, title, message)
        if (!suggestedName.isNullOrBlank()) {
            msgVoid(panel, SEL_setNameFieldStringValue, nsStringFromUtf8(suggestedName))
        }

        if (msgLong(panel, SEL_runModal) != 1L) null else collectSingleURL(panel)
    }

    // ---- Internals ----

    private fun setFilters(panel: Pointer, filters: KmpFileFilter) {
        val normalized = filters.map { it.extension.trim().removePrefix(".") }.filter { it.isNotEmpty() }
        if (normalized.isNotEmpty()) {
            msgVoid(panel, SEL_setAllowedFileTypes, nsArrayOfStrings(normalized))
        }
    }

    private fun setStartDir(panel: Pointer, start: KmpFsRef?) {
//        if (start != null && start.scheme == "file") {
//            val path = Paths.get(start).toString()
//            msgVoid(panel, SEL_setDirectoryURL, nsUrlFileURLWithPath(path))
//        }
    }

    private fun setTitleAndMessage(panel: Pointer, title: String?, message: String?) {
        if (!title.isNullOrBlank()) msgVoid(panel, SEL_setTitle, nsStringFromUtf8(title))
        if (!message.isNullOrBlank()) msgVoid(panel, SEL_setMessage, nsStringFromUtf8(message))
    }

    private fun collectURLs(panel: Pointer, multiple: Boolean): List<KmpFsRef> {
        return if (multiple) {
            val urls = msgPtr(panel, SEL_URLs)!!
            val count = msgLong(urls, SEL_count).toInt()
            (0 until count).mapNotNull { i ->
                val url = msgPtr(urls, SEL_objectAtIndex, i) ?: return@mapNotNull null
                urlToUri(url)
            }
        } else {
            collectSingleURL(panel)?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun collectSingleURL(panel: Pointer): KmpFsRef? {
        val url = msgPtr(panel, SEL_URL) ?: return null
        return urlToUri(url)
    }

    // NSURL -> URI via path (file://)
    private fun urlToUri(nsUrl: Pointer): KmpFsRef? {
        val nsPath = msgPtr(nsUrl, SEL_path) ?: return null
        val cStr = msgPtr(nsPath, SEL_UTF8String) ?: return null
        val path = cStr.getString(0, StandardCharsets.UTF_8.name())
        return KmpFsRef(
            ref = Paths.get(path).toUri().toString(),
            isDirectory = false,
            name = "",
            fsType = KmpFsType.External,
        )
    }

    // ---- ObjC helpers ----
    private fun sel(name: String): Pointer = sel_registerName.invokePointer(arrayOf(name))!!
    private fun cls(name: String): Pointer = objc_getClass.invokePointer(arrayOf(name))!!
    private fun msgPtr(rcv: Pointer, sel: Pointer, vararg args: Any?): Pointer? =
        objc_msgSend.invokePointer(arrayOf(rcv, sel, *args))
    private fun msgLong(rcv: Pointer, sel: Pointer, vararg args: Any?): Long =
        objc_msgSend.invokeLong(arrayOf(rcv, sel, *args))
    private fun msgVoid(rcv: Pointer, sel: Pointer, vararg args: Any?) {
        objc_msgSend.invokeVoid(arrayOf(rcv, sel, *args))
    }

    private fun nsStringFromUtf8(s: String): Pointer {
        val bytes = Native.toByteArray(s, StandardCharsets.UTF_8.name())
        return msgPtr(CLS_NSString, SEL_stringWithUTF8String, bytes)!!
    }

    private fun nsArrayOfStrings(items: List<String>): Pointer {
        val nsStrings = items.map { nsStringFromUtf8(it) }.toTypedArray()
        return msgPtr(
            CLS_NSArray,
            SEL_arrayWithObjects_count,
            nsStrings, // JNA will pass as Pointer[]
            nsStrings.size,
        )!!
    }

    private fun nsUrlFileURLWithPath(path: String): Pointer {
        val nsPath = nsStringFromUtf8(path)
        return msgPtr(CLS_NSURL, SEL_fileURLWithPath, nsPath)!!
    }

    // Autorelease pool scope
    private inline fun <T> withPool(block: () -> T): T {
        val pool = msgPtr(msgPtr(CLS_NSAutoreleasePool, SEL_alloc)!!, SEL_init)!!
        try { return block() } finally { msgVoid(pool, SEL_drain) }
    }

    private class NativeLibraryHolder(name: String) {
        private val lib = com.sun.jna.NativeLibrary.getInstance(name)
        fun func(symbol: String): com.sun.jna.Function = lib.getFunction(symbol)
    }
}

object CocoaMain {
    private val objc = NativeLibrary.getInstance("objc")
    private val objc_msgSend = objc.getFunction("objc_msgSend")
    private val objc_getClass = objc.getFunction("objc_getClass")
    private val sel_registerName = objc.getFunction("sel_registerName")
    private val objc_allocateClassPair = objc.getFunction("objc_allocateClassPair")
    private val objc_registerClassPair = objc.getFunction("objc_registerClassPair")
    private val class_addMethod = objc.getFunction("class_addMethod")
    private val CLS_NSAutoreleasePool = cls("NSAutoreleasePool")
    private val SEL_alloc = sel("alloc")
    private val SEL_init = sel("init")
    private val SEL_drain = sel("drain")

    private inline fun <T> withAutoreleasePool(block: () -> T): T {
        val pool = msgPtr(msgPtr(CLS_NSAutoreleasePool, SEL_alloc)!!, SEL_init)!!
        try { return block() } finally { msgVoid(pool, SEL_drain) }
    }

    private fun sel(name: String): Pointer =
        sel_registerName.invokePointer(arrayOf(name))!!

    private fun cls(name: String): Pointer =
        objc_getClass.invokePointer(arrayOf(name))!!

    private fun msgPtr(rcv: Pointer, s: Pointer, vararg args: Any?): Pointer? =
        objc_msgSend.invokePointer(arrayOf(rcv, s, *args))

    private fun msgVoid(rcv: Pointer, s: Pointer, vararg args: Any?) =
        objc_msgSend.invokeVoid(arrayOf(rcv, s, *args))

    private fun msgInt(rcv: Pointer, s: Pointer, vararg args: Any?): Int =
        objc_msgSend.invokeInt(arrayOf(rcv, s, *args))

    // --- selectors/classes we use ---
    private val SEL_isMainThread = sel("isMainThread")
    private val CLS_NSThread = cls("NSThread")

    fun isMainThread(): Boolean = msgInt(CLS_NSThread, SEL_isMainThread) != 0

    private val SEL_new = sel("new")
    private val SEL_release = sel("release")
    private val SEL_performOnMain =
        sel("performSelectorOnMainThread:withObject:waitUntilDone:")
    private val SEL_run = sel("run:")

    // Our dynamic Objective‑C class: subclass of NSObject with a -run:(id) method.
    private val runnerClass: Pointer by lazy {
        val NSObject = cls("NSObject")
        val name = "KMPRunner_${System.identityHashCode(this)}"
        val newCls = objc_allocateClassPair.invokePointer(arrayOf(NSObject, name, 0))!!
        // type encoding: "v@:@"  -> void, self, _cmd, id
        val types = "v@:@"
        class_addMethod.invokeInt(arrayOf(newCls, SEL_run, runCallback as Callback, types))
        objc_registerClassPair.invokeVoid(arrayOf(newCls))
        newCls
    }

    // Map from instance pointer to Runnable
    private val tasks = ConcurrentHashMap<Long, Runnable>()

    private fun interface ObjCImpRun : Callback {
        fun invoke(self: Pointer?, _cmd: Pointer?, arg: Pointer?)
    }

    // IMP for -run:
    // Signature: void run(id self, SEL _cmd, id arg)
    private val runCallback = ObjCImpRun { self: Pointer?, p: Pointer?, p2: Pointer? ->
        withAutoreleasePool {
            try {
                val key = Pointer.nativeValue(self)
                tasks.remove(key)?.run()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                // Balance the +new retain; safe to release on main thread after running
                if (self != null) msgVoid(self, SEL_release)
            }
        }
    }

    private fun newRunner(): Pointer =
        msgPtr(runnerClass, SEL_new)!! // retained (+1); released in callback

    /**
     * Execute [block] on the AppKit main thread. If [wait] is true, block until it finishes.
     *
     * NOTE: This queues work to the AppKit main runloop. Your host app should have
     * a functioning Cocoa main thread (e.g., launched with -XstartOnFirstThread or via
     * a native/Compose launcher). If there's no runloop, the work won't execute.
     */
    fun executeOnMainThread(wait: Boolean = true, block: () -> Unit) {
        // Fast path: already on main & waiting -> run inline
        if (wait && isMainThread()) {
            block()
            return
        }
        // Create an instance, stash the job keyed by its pointer
        val runner = newRunner()
        tasks[Pointer.nativeValue(runner)] = Runnable { block() }

        // [runner performSelectorOnMainThread:@selector(run:) withObject:nil waitUntilDone:wait]
        msgVoid(
            runner,
            SEL_performOnMain,
            SEL_run,
            Pointer.NULL,
            if (wait) 1 else 0,
        )
        // If wait=false, this returns immediately; the instance is retained until callback runs.
    }
}
