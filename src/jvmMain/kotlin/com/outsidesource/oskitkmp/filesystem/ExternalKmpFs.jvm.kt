package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path.Companion.toPath
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.FileDialog

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
            if (Platform.current == Platform.Linux) return nativeOpenFilePicker(startingDir, filter)

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

    private fun nativeOpenFilePicker(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        val file = MemoryStack.stackPush().use { stack ->
            val filters = stack.mallocPointer(filter?.size ?: 0)
            for (fileFilter in filter ?: emptyList()) {
                filters.put(stack.UTF8("*.${fileFilter.extension}"))
            }
            filters.flip()

            TinyFileDialogs.tinyfd_openFileDialog(
                "Select File",
                startingDir?.ref ?: "",
                filters,
                null,
                false,
            )
        } ?: return Outcome.Ok(null)

        return Outcome.Ok(
            KmpFsRef(ref = file, name = file.toPath().name, isDirectory = false, fsType = KmpFsType.External),
        )
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
            if (Platform.current == Platform.Linux) return nativeOpenFilesPicker(startingDir, filter)

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

    private fun nativeOpenFilesPicker(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        val files = MemoryStack.stackPush().use { stack ->
            val filters = stack.mallocPointer(filter?.size ?: 0)
            for (fileFilter in filter ?: emptyList()) {
                filters.put(stack.UTF8("*.${fileFilter.extension}"))
            }
            filters.flip()

            TinyFileDialogs.tinyfd_openFileDialog(
                "Select Files",
                startingDir?.ref ?: "",
                filters,
                null,
                true,
            )
        } ?: return Outcome.Ok(null)

        val refs = files.split("|").map { file ->
            KmpFsRef(
                ref = file,
                name = file.toPath().name,
                isDirectory = false,
                fsType = KmpFsType.External,
            )
        }

        return Outcome.Ok(refs)
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        if (context == null) return Outcome.Error(KmpFsError.NotInitialized)
        if (startingDir != null && !startingDir.isDirectory) return Outcome.Error(KmpFsError.RefIsNotDirectory)
        if (startingDir != null && startingDir.fsType != KmpFsType.External) return Outcome.Error(KmpFsError.RefFsType)

        return try {
            // Use TinyFileDialogs because there is no AWT directory picker
            val directory = TinyFileDialogs.tinyfd_selectFolderDialog("Select Folder", startingDir?.ref ?: "")
                ?: return Outcome.Ok(null)
            val ref = KmpFsRef(
                ref = directory,
                name = directory.toPath().name,
                isDirectory = true,
                fsType = KmpFsType.External,
            )

            Outcome.Ok(ref)
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
            if (Platform.current == Platform.Linux) return nativeSaveFilePicker(fileName, startingDir)

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

    private fun nativeSaveFilePicker(
        name: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        val file = TinyFileDialogs.tinyfd_saveFileDialog(
            "Save File",
            joinPathSegments(startingDir?.ref ?: "", name),
            null,
            null,
        ) ?: return Outcome.Ok(null)

        return Outcome.Ok(
            KmpFsRef(ref = file, name = file.toPath().name, isDirectory = false, fsType = KmpFsType.External),
        )
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
