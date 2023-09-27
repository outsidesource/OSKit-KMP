package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.lib.pathString
import com.outsidesource.oskitkmp.outcome.Outcome
import okio.*
import okio.Path.Companion.toPath
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.FileDialog
import java.awt.Frame

actual class KMPFileHandlerContext(val window: Frame)

class DesktopKMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?
    ): Outcome<KMPFileRef?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val dialog = FileDialog(context.window, "Select File", FileDialog.LOAD)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            if (filter != null) dialog.setFilenameFilter { _, name -> filter.any { name.endsWith(it.extension) } }
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KMPFileRef(
                ref = "${dialog.directory}${dialog.file}",
                name = dialog.file,
                isDirectory = false,
            )

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun pickDirectory(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        return try {
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
            val context = context ?: return Outcome.Error(NotInitializedException())
            val dialog = FileDialog(context.window, "Save File", FileDialog.SAVE)
            dialog.directory = startingDir?.ref?.toPath()?.pathString
            dialog.file = fileName
            dialog.isVisible = true

            if (dialog.file == null) return Outcome.Ok(null)

            val ref = KMPFileRef(
                ref = "${dialog.directory}${dialog.file}",
                name = dialog.file,
                isDirectory = false,
            )

            FileSystem.SYSTEM.sink(ref.ref.toPath(), mustCreate = true)

            Outcome.Ok(ref)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveFile(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        return try {
            val path = "${dir.ref}$name".toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(FileNotFoundException())
            if (create) FileSystem.SYSTEM.sink(path, mustCreate = true)

            return Outcome.Ok(KMPFileRef(ref = path.pathString, name = name, isDirectory = false))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        return try {
            val path = "${dir.ref}$name".toPath()
            val exists = FileSystem.SYSTEM.exists(path)

            if (!exists && !create) return Outcome.Error(FileNotFoundException())
            if (create) FileSystem.SYSTEM.createDirectory(path, mustCreate = true)

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
