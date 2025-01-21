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
    private var context: KmpFsContext? = null

    override fun init(fileHandlerContext: KmpFsContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, KmpFsError> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeOpenFilePicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(KmpFsError.NotInitializedError)
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KmpFsRef(
                ref = joinPathSegments(dialog.directory, dialog.file),
                name = dialog.file,
                isDirectory = false,
                type = KmpFsType.External,
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
            KmpFsRef(ref = file, name = file.toPath().name, isDirectory = false, type = KmpFsType.External),
        )
    }

    override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, KmpFsError> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeOpenFilesPicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(KmpFsError.NotInitializedError)
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
                    type = KmpFsType.External,
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
                type = KmpFsType.External,
            )
        }

        return Outcome.Ok(refs)
    }

    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError> {
        return try {
            // Use TinyFileDialogs because there is no AWT directory picker
            val directory = TinyFileDialogs.tinyfd_selectFolderDialog("Select Folder", startingDir?.ref ?: "")
                ?: return Outcome.Ok(null)
            val ref = KmpFsRef(
                ref = directory,
                name = directory.toPath().name,
                isDirectory = true,
                type = KmpFsType.External,
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
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeSaveFilePicker(fileName, startingDir)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(KmpFsError.NotInitializedError)
            val dialog = FileDialog(context.window, "Save File", FileDialog.SAVE)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.file = fileName
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KmpFsRef(
                ref = joinPathSegments(dialog.directory, dialog.file),
                name = dialog.file,
                isDirectory = false,
                type = KmpFsType.External,
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
            KmpFsRef(ref = file, name = file.toPath().name, isDirectory = false, type = KmpFsType.External),
        )
    }

    override suspend fun resolveFile(
        dir: KmpFsRef,
        fileName: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        return try {
            val path = joinPathSegments(dir.ref, fileName).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(KmpFsError.NotFoundError)
            if (create) FileSystem.SYSTEM.sink(path, mustCreate = !exists)

            return Outcome.Ok(
                KmpFsRef(ref = path.pathString, name = fileName, isDirectory = false, type = KmpFsType.External),
            )
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, KmpFsError> {
        return try {
            val path = joinPathSegments(dir.ref, name).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(KmpFsError.NotFoundError)
            if (create) FileSystem.SYSTEM.createDirectory(path, mustCreate = !exists)

            return Outcome.Ok(
                KmpFsRef(ref = path.pathString, name = name, isDirectory = true, type = KmpFsType.External),
            )
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun saveFile(
        bytes: ByteArray,
        fileName: String,
    ): Outcome<Unit, KmpFsError> = nonJsSaveFile(bytes, fileName)

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError> {
        return try {
            val localPath = path.toPath()
            val exists = FileSystem.SYSTEM.exists(localPath)

            if (!exists) return Outcome.Error(KmpFsError.NotFoundError)
            val metadata = FileSystem.SYSTEM.metadata(localPath)

            val ref = KmpFsRef(
                ref = localPath.pathString,
                name = localPath.name,
                isDirectory = metadata.isDirectory,
                type = KmpFsType.External,
            )
            return Outcome.Ok(ref)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError> {
        return try {
            FileSystem.SYSTEM.deleteRecursively(ref.ref.toPath())
            Outcome.Ok(Unit)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, KmpFsError> {
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())
            val path = dir.ref.toPath()

            val list = if (isRecursive) {
                FileSystem.SYSTEM.listRecursively(path).toList()
            } else {
                FileSystem.SYSTEM.list(path)
            }.mapNotNull {
                val metadata = FileSystem.SYSTEM.metadataOrNull(it) ?: return@mapNotNull null

                KmpFsRef(
                    ref = it.pathString,
                    name = it.name,
                    isDirectory = metadata.isDirectory,
                    type = KmpFsType.External,
                )
            }

            return Outcome.Ok(list)
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError> {
        return try {
            val path = ref.ref.toPath()
            val metadata = FileSystem.SYSTEM.metadata(path)
            val size = metadata.size ?: return Outcome.Error(KmpFsError.MetadataError)
            Outcome.Ok(KmpFileMetadata(size = size))
        } catch (t: Throwable) {
            Outcome.Error(KmpFsError.Unknown(t))
        }
    }

    override suspend fun exists(ref: KmpFsRef): Boolean {
        return try {
            FileSystem.SYSTEM.exists(ref.ref.toPath())
        } catch (t: Throwable) {
            false
        }
    }
}
