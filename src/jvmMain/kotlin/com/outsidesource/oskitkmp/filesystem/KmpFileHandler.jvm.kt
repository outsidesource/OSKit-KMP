package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.FileDialog
import java.awt.Frame

actual class KmpFileHandlerContext(val window: Frame)

actual class KmpFileHandler : IKmpFileHandler {
    private var context: KmpFileHandlerContext? = null
    private val pathSeparatorChars = Path.DIRECTORY_SEPARATOR.toCharArray()

    actual override fun init(fileHandlerContext: KmpFileHandlerContext) {
        context = fileHandlerContext
    }

    actual override suspend fun pickFile(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeOpenFilePicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(NotInitializedError())
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KmpFileRef(
                ref = joinDirectoryAndFilePath(dialog.directory, dialog.file),
                name = dialog.file,
                isDirectory = false,
            )

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private fun nativeOpenFilePicker(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception> {
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

        return Outcome.Ok(KmpFileRef(ref = file, name = file.toPath().name, isDirectory = false))
    }

    actual override suspend fun pickFiles(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFileRef>?, Exception> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeOpenFilesPicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(NotInitializedError())
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.isMultipleMode = true
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.files == null || dialog.files.isEmpty()) return Outcome.Ok(null)

            val refs = dialog.files.map { file ->
                KmpFileRef(
                    ref = joinDirectoryAndFilePath(dialog.directory, file.name),
                    name = file.name,
                    isDirectory = false,
                )
            }

            Outcome.Ok(refs)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private fun nativeOpenFilesPicker(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFileRef>?, Exception> {
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
            KmpFileRef(
                ref = file,
                name = file.toPath().name,
                isDirectory = false,
            )
        }

        return Outcome.Ok(refs)
    }

    actual override suspend fun pickDirectory(startingDir: KmpFileRef?): Outcome<KmpFileRef?, Exception> {
        return try {
            // Use TinyFileDialogs because there is no AWT directory picker
            val directory = TinyFileDialogs.tinyfd_selectFolderDialog("Select Folder", startingDir?.ref ?: "")
                ?: return Outcome.Ok(null)
            val ref = KmpFileRef(
                ref = directory,
                name = directory.toPath().name,
                isDirectory = true,
            )

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFileRef?,
    ): Outcome<KmpFileRef?, Exception> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeSaveFilePicker(fileName, startingDir)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(NotInitializedError())
            val dialog = FileDialog(context.window, "Save File", FileDialog.SAVE)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.file = fileName
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KmpFileRef(
                ref = joinDirectoryAndFilePath(dialog.directory, dialog.file),
                name = dialog.file,
                isDirectory = false,
            )

            FileSystem.SYSTEM.sink(ref.ref.toPath(), mustCreate = true)

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    private fun nativeSaveFilePicker(
        name: String,
        startingDir: KmpFileRef?,
    ): Outcome<KmpFileRef?, Exception> {
        val file = TinyFileDialogs.tinyfd_saveFileDialog(
            "Save File",
            joinDirectoryAndFilePath(startingDir?.ref ?: "", name),
            null,
            null,
        ) ?: return Outcome.Ok(null)

        return Outcome.Ok(KmpFileRef(ref = file, name = file.toPath().name, isDirectory = false))
    }

    actual override suspend fun resolveFile(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        return try {
            val path = joinDirectoryAndFilePath(dir.ref, name).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(FileNotFoundError())
            if (create) FileSystem.SYSTEM.sink(path, mustCreate = !exists)

            return Outcome.Ok(KmpFileRef(ref = path.pathString, name = name, isDirectory = false))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        return try {
            val path = joinDirectoryAndFilePath(dir.ref, name).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(FileNotFoundError())
            if (create) FileSystem.SYSTEM.createDirectory(path, mustCreate = !exists)

            return Outcome.Ok(KmpFileRef(ref = path.pathString, name = name, isDirectory = true))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception> {
        return try {
            val localPath = path.toPath()
            val exists = FileSystem.SYSTEM.exists(localPath)

            if (!exists) return Outcome.Error(FileNotFoundError())
            val metadata = FileSystem.SYSTEM.metadata(localPath)

            val ref = KmpFileRef(ref = localPath.pathString, name = localPath.name, isDirectory = metadata.isDirectory)
            return Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception> {
        return try {
            FileSystem.SYSTEM.deleteRecursively(ref.ref.toPath())
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun list(dir: KmpFileRef, isRecursive: Boolean): Outcome<List<KmpFileRef>, Exception> {
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())
            val path = dir.ref.toPath()

            val list = if (isRecursive) {
                FileSystem.SYSTEM.listRecursively(path).toList()
            } else {
                FileSystem.SYSTEM.list(path)
            }.mapNotNull {
                val metadata = FileSystem.SYSTEM.metadataOrNull(it) ?: return@mapNotNull null

                KmpFileRef(
                    ref = it.pathString,
                    name = it.name,
                    isDirectory = metadata.isDirectory,
                )
            }

            return Outcome.Ok(list)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun readMetadata(ref: KmpFileRef): Outcome<KmpFileMetadata, Exception> {
        return try {
            val path = ref.ref.toPath()
            val metadata = FileSystem.SYSTEM.metadata(path)
            val size = metadata.size ?: return Outcome.Error(FileMetadataError())
            Outcome.Ok(KmpFileMetadata(size = size))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun exists(ref: KmpFileRef): Boolean {
        return try {
            FileSystem.SYSTEM.exists(ref.ref.toPath())
        } catch (e: Exception) {
            false
        }
    }

    // Makes sure there is a path separator when joining a directory and file path. Some platforms (linux) may not
    // include the trailing / when selecting a directory
    private fun joinDirectoryAndFilePath(dir: String, name: String): String =
        dir.trimEnd(*pathSeparatorChars) + Path.DIRECTORY_SEPARATOR + name
}

actual suspend fun KmpFileRef.source(): Outcome<IKmpFsAsyncSource, Exception> {
    return try {
        if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
        val source = FileSystem.SYSTEM.source(ref.toPath())
        Outcome.Ok(KmpFsOkIoAsyncSource(source))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

actual suspend fun KmpFileRef.sink(mode: KmpFileWriteMode): Outcome<IKmpFsAsyncSink, Exception> {
    return try {
        if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
        val sink = if (mode == KmpFileWriteMode.Append) {
            FileSystem.SYSTEM.appendingSink(ref.toPath())
        } else {
            FileSystem.SYSTEM.sink(ref.toPath())
        }
        Outcome.Ok(KmpFsOkIoAsyncSink(sink))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
