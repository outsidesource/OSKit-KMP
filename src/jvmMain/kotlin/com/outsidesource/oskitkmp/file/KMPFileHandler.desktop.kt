package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current
import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.FileDialog
import java.awt.Frame

actual class KMPFileHandlerContext(val window: Frame)

actual class KMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null
    private val pathSeparatorChars = Path.DIRECTORY_SEPARATOR.toCharArray()

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?,
    ): Outcome<KMPFileRef?, Exception> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeOpenFilePicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(NotInitializedException())
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KMPFileRef(
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
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?,
    ): Outcome<KMPFileRef?, Exception> {
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

        return Outcome.Ok(KMPFileRef(ref = file, name = file.toPath().name, isDirectory = false))
    }

    override suspend fun pickFiles(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?,
    ): Outcome<List<KMPFileRef>?, Exception> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeOpenFilesPicker(startingDir, filter)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(NotInitializedException())
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.isMultipleMode = true
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.files == null || dialog.files.isEmpty()) return Outcome.Ok(null)

            val refs = dialog.files.map { file ->
                KMPFileRef(
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
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?,
    ): Outcome<List<KMPFileRef>?, Exception> {
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
            KMPFileRef(
                ref = file,
                name = file.toPath().name,
                isDirectory = false,
            )
        }

        return Outcome.Ok(refs)
    }

    override suspend fun pickDirectory(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        return try {
            // Use TinyFileDialogs because there is no AWT directory picker
            val directory = TinyFileDialogs.tinyfd_selectFolderDialog("Select Folder", startingDir?.ref ?: "")
                ?: return Outcome.Ok(null)
            val ref = KMPFileRef(
                ref = directory,
                name = directory.toPath().name,
                isDirectory = true,
            )

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun pickSaveFile(fileName: String, startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        return try {
            // Prefer native file picker on linux due to issue with libfreetype in Plasma on Linux
            if (Platform.current == Platform.Linux) return nativeSaveFilePicker(fileName, startingDir)

            // Prefer FileDialog on other platforms. On MacOS, TinyFileDialogs does not allow other windows to be focused
            val context = context ?: return Outcome.Error(NotInitializedException())
            val dialog = FileDialog(context.window, "Save File", FileDialog.SAVE)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.file = fileName
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KMPFileRef(
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
        startingDir: KMPFileRef?,
    ): Outcome<KMPFileRef?, Exception> {
        val file = TinyFileDialogs.tinyfd_saveFileDialog(
            "Save File",
            joinDirectoryAndFilePath(startingDir?.ref ?: "", name),
            null,
            null,
        ) ?: return Outcome.Ok(null)

        return Outcome.Ok(KMPFileRef(ref = file, name = file.toPath().name, isDirectory = false))
    }

    override suspend fun resolveFile(
        dir: KMPFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KMPFileRef, Exception> {
        return try {
            val path = joinDirectoryAndFilePath(dir.ref, name).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(FileNotFoundException())
            if (create) FileSystem.SYSTEM.sink(path, mustCreate = !exists)

            return Outcome.Ok(KMPFileRef(ref = path.pathString, name = name, isDirectory = false))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KMPFileRef, Exception> {
        return try {
            val path = joinDirectoryAndFilePath(dir.ref, name).toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(FileNotFoundException())
            if (create) FileSystem.SYSTEM.createDirectory(path, mustCreate = !exists)

            return Outcome.Ok(KMPFileRef(ref = path.pathString, name = name, isDirectory = true))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveRefFromPath(path: String): Outcome<KMPFileRef, Exception> {
        return try {
            val localPath = path.toPath()
            val exists = FileSystem.SYSTEM.exists(localPath)

            if (!exists) return Outcome.Error(FileNotFoundException())
            val metadata = FileSystem.SYSTEM.metadata(localPath)

            val ref = KMPFileRef(ref = localPath.pathString, name = localPath.name, isDirectory = metadata.isDirectory)
            return Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception> {
        return try {
            FileSystem.SYSTEM.delete(ref.ref.toPath())
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun list(dir: KMPFileRef, isRecursive: Boolean): Outcome<List<KMPFileRef>, Exception> {
        return try {
            if (!dir.isDirectory) return Outcome.Ok(emptyList())
            val path = dir.ref.toPath()

            val list = if (isRecursive) {
                FileSystem.SYSTEM.listRecursively(path).toList()
            } else {
                FileSystem.SYSTEM.list(path)
            }.mapNotNull {
                val metadata = FileSystem.SYSTEM.metadataOrNull(it) ?: return@mapNotNull null

                KMPFileRef(
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

    override suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception> {
        return try {
            val path = ref.ref.toPath()
            val metadata = FileSystem.SYSTEM.metadata(path)
            val size = metadata.size ?: return Outcome.Error(FileMetadataException())
            Outcome.Ok(KMPFileMetadata(size = size))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun exists(ref: KMPFileRef): Boolean {
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

actual fun KMPFileRef.source(): Outcome<Source, Exception> {
    return try {
        if (isDirectory) return Outcome.Error(SourceException())
        Outcome.Ok(FileSystem.SYSTEM.source(ref.toPath()))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

actual fun KMPFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return try {
        if (isDirectory) return Outcome.Error(SinkException())
        if (mode == KMPFileWriteMode.Append) {
            Outcome.Ok(FileSystem.SYSTEM.appendingSink(ref.toPath()))
        } else {
            Outcome.Ok(FileSystem.SYSTEM.sink(ref.toPath()))
        }
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
