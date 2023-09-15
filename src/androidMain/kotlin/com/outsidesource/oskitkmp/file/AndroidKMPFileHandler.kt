package com.outsidesource.oskitkmp.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.outsidesource.oskitkmp.lib.fileSystem
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.getOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okio.FileMetadata
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source

actual data class KMPFileHandlerContext(
    val applicationContext: Context,
    val activity: ComponentActivity,
)

class AndroidKMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var openFileResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private val openFileResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var openFolderResultLauncher: ActivityResultLauncher<Uri?>? = null
    private val openFolderResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext

        openFileResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { data ->
            coroutineScope.launch {
                openFileResultFlow.emit(data)
            }
        }

        openFolderResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { data ->
            coroutineScope.launch {
                openFolderResultFlow.emit(data)
            }
        }
    }

    override suspend fun pickFile(
        startingDir: KMPFileURI?,
        filter: KMPFileFilter?
    ): Outcome<KMPFileURI?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val fileResultLauncher = openFileResultLauncher ?: return Outcome.Error(NotInitializedException())

            fileResultLauncher.launch(filter?.map { it.mimeType }?.toTypedArray() ?: arrayOf("*/*"))
            val uri = openFileResultFlow.firstOrNull() ?: return Outcome.Ok(null)

            val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenException())

            Outcome.Ok(KMPFileURI(uri = uri.path.toString(), isDirectory = false, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun pickFolder(startingDir: KMPFileURI?): Outcome<KMPFileURI?, Exception> {
        return try {
            val folderResultLauncher = openFolderResultLauncher ?: return Outcome.Error(NotInitializedException())
            val context = context ?: return Outcome.Error(NotInitializedException())

            folderResultLauncher.launch(startingDir?.uri?.toUri())
            val uri = openFolderResultFlow.firstOrNull() ?: return Outcome.Ok(null)
            val name = DocumentFile.fromTreeUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenException())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KMPFileURI(uri = uri.toString(), isDirectory = true, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveFile(
        dir: KMPFileURI,
        name: String,
        mustCreate: Boolean
    ): Outcome<KMPFileURI, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val parentUri = DocumentFile.fromTreeUri(context.applicationContext, dir.uri.toUri())
                ?: return Outcome.Error(FileOpenException())
            val file = parentUri.findFile(name)
            if (file == null && !mustCreate) return Outcome.Error(FileNotFoundException())
            val createdFile = file ?: parentUri.createFile("", name)
                ?: return Outcome.Error(FileCreateException())

            Outcome.Ok(KMPFileURI(uri = createdFile.uri.toString(), name = name, isDirectory = false))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun resolveDirectory(
        dir: KMPFileURI,
        name: String,
        mustCreate: Boolean
    ): Outcome<KMPFileURI, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val parentUri = DocumentFile.fromTreeUri(context.applicationContext, dir.uri.toUri())
                ?: return Outcome.Error(FileOpenException())
            val file = parentUri.findFile(name)
            if (file == null && !mustCreate) return Outcome.Error(FileNotFoundException())
            if (file != null && file.isDirectory) return Outcome.Error(FileCreateException())
            val createdFile = file ?: parentUri.createFile("", name)
                ?: return Outcome.Error(FileCreateException())

            Outcome.Ok(KMPFileURI(uri = createdFile.uri.toString(), name = name, isDirectory = true))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    // TODO: Test renaming the root directory and see if references are still valid
    // TODO: Test renaming directory and file
    override suspend fun rename(file: KMPFileURI, name: String): Outcome<KMPFileURI, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val documentFile = if (file.isDirectory) {
                DocumentFile.fromTreeUri(context.applicationContext, file.uri.toUri())
                    ?: return Outcome.Error(FileOpenException())
            } else {
                DocumentFile.fromSingleUri(context.applicationContext, file.uri.toUri())
                    ?: return Outcome.Error(FileOpenException())
            }

            if (!documentFile.renameTo(name)) return Outcome.Error(FileRenameException())

            return Outcome.Ok(file.copy(uri = documentFile.uri.toString(), name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun delete(file: KMPFileURI, isRecursive: Boolean): Outcome<Unit, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val documentFile = if (file.isDirectory) {
                DocumentFile.fromTreeUri(context.applicationContext, file.uri.toUri())
                    ?: return Outcome.Error(FileOpenException())
            } else {
                DocumentFile.fromSingleUri(context.applicationContext, file.uri.toUri())
                    ?: return Outcome.Error(FileOpenException())
            }

            if (!documentFile.delete()) return Outcome.Error(FileDeleteException())

            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    // TODO: Test recursive
    override suspend fun list(dir: KMPFileURI, isRecursive: Boolean): Outcome<List<KMPFileURI>, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val documentFile = DocumentFile.fromTreeUri(context.applicationContext, dir.uri.toUri())
                ?: return Outcome.Error(FileOpenException())
            if (!isRecursive) {
                val list = documentFile.listFiles().map {
                    KMPFileURI(uri = it.uri.toString(), name = it.name ?: "", isDirectory = it.isDirectory)
                }
                return Outcome.Ok(list)
            }

            val list = documentFile.listFiles().flatMap {
                buildList {
                    val file = KMPFileURI(uri = it.uri.toString(), name = it.name ?: "", isDirectory = it.isDirectory)
                    add(file)

                    if (it.isDirectory) {
                        addAll(list(file).getOrNull() ?: emptyList())
                    }
                }
            }

            return Outcome.Ok(list)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun readMetadata(file: KMPFileURI): Outcome<FileMetadata, Exception> {
        return try {
            return Outcome.Ok(fileSystem.metadata(file.uri.toPath()))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun exists(file: KMPFileURI): Boolean {
        return fileSystem.exists(file.uri.toPath())
    }
}

actual fun KMPFileURI.source(): Outcome<Source, Exception> {
    return try {
        Outcome.Ok(fileSystem.source(uri.toPath()))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

actual fun KMPFileURI.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return try {
        val sink = when (mode) {
            KMPFileWriteMode.Append -> fileSystem.appendingSink(uri.toPath(), mustExist = true)
            KMPFileWriteMode.Overwrite -> fileSystem.sink(uri.toPath(), mustCreate = false)
        }
        Outcome.Ok(sink)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
