package com.outsidesource.oskitkmp.file

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okio.Sink
import okio.Source
import okio.sink
import okio.source

actual data class KMPFileHandlerContext(
    val applicationContext: Context,
    val activity: ComponentActivity,
)

class AndroidKMPFileHandler : IKMPFileHandler {
    companion object {
        internal var context: KMPFileHandlerContext? = null
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var pickFileResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private val pickFileResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var pickSaveFileResultLauncher: ActivityResultLauncher<String?>? = null
    private val pickSaveFileResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var pickFolderResultLauncher: ActivityResultLauncher<Uri?>? = null
    private val pickFolderResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext

        pickFileResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { data ->
            coroutineScope.launch {
                pickFileResultFlow.emit(data)
            }
        }

        pickSaveFileResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("*/*")
        ) { data ->
            coroutineScope.launch {
                pickSaveFileResultFlow.emit(data)
            }
        }

        pickFolderResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { data ->
            coroutineScope.launch {
                pickFolderResultFlow.emit(data)
            }
        }
    }

    override suspend fun pickFile(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?
    ): Outcome<KMPFileRef?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val fileResultLauncher = pickFileResultLauncher ?: return Outcome.Error(NotInitializedException())

            fileResultLauncher.launch(filter?.map { it.mimeType }?.toTypedArray() ?: arrayOf("*/*"))
            val uri = pickFileResultFlow.firstOrNull() ?: return Outcome.Ok(null)

            val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenException())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KMPFileRef(ref = uri.toString(), isDirectory = false, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun pickSaveFile(fileName: String, startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val fileResultLauncher = pickSaveFileResultLauncher ?: return Outcome.Error(NotInitializedException())

            fileResultLauncher.launch(fileName)
            val uri = pickSaveFileResultFlow.firstOrNull() ?: return Outcome.Ok(null)

            val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenException())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KMPFileRef(ref = uri.toString(), isDirectory = false, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun pickDirectory(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        return try {
            val folderResultLauncher = pickFolderResultLauncher ?: return Outcome.Error(NotInitializedException())
            val context = context ?: return Outcome.Error(NotInitializedException())

            folderResultLauncher.launch(startingDir?.ref?.toUri())
            val uri = pickFolderResultFlow.firstOrNull() ?: return Outcome.Ok(null)
            val name = DocumentFile.fromTreeUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenException())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KMPFileRef(ref = uri.toString(), isDirectory = true, name = name))
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
            val context = context ?: return Outcome.Error(NotInitializedException())
            val parentUri = DocumentFile.fromTreeUri(context.applicationContext, dir.ref.toUri())
                ?: return Outcome.Error(FileOpenException())
            val file = parentUri.findFile(name)
            if (file == null && !create) return Outcome.Error(FileNotFoundException())
            val createdFile = file ?: parentUri.createFile("", name)
                ?: return Outcome.Error(FileCreateException())

            Outcome.Ok(KMPFileRef(ref = createdFile.uri.toString(), name = name, isDirectory = false))
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
            val context = context ?: return Outcome.Error(NotInitializedException())
            val parentUri = DocumentFile.fromTreeUri(context.applicationContext, dir.ref.toUri())
                ?: return Outcome.Error(FileOpenException())
            val file = parentUri.findFile(name)
            if (file == null && !create) return Outcome.Error(FileNotFoundException())
            if (file != null && file.isDirectory) return Outcome.Error(FileCreateException())
            val createdDirectory = file ?: parentUri.createDirectory(name)
                ?: return Outcome.Error(FileCreateException())

            Outcome.Ok(KMPFileRef(ref = createdDirectory.uri.toString(), name = name, isDirectory = true))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val documentFile = if (ref.isDirectory) {
                DocumentFile.fromTreeUri(context.applicationContext, ref.ref.toUri())
                    ?: return Outcome.Error(FileOpenException())
            } else {
                DocumentFile.fromSingleUri(context.applicationContext, ref.ref.toUri())
                    ?: return Outcome.Error(FileOpenException())
            }

            if (!documentFile.delete()) return Outcome.Error(FileDeleteException())

            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun list(dir: KMPFileRef, isRecursive: Boolean): Outcome<List<KMPFileRef>, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val documentFile = DocumentFile.fromTreeUri(context.applicationContext, dir.ref.toUri())
                ?: return Outcome.Error(FileOpenException())

            if (!isRecursive) {
                val list = documentFile.listFiles().map {
                    KMPFileRef(ref = it.uri.toString(), name = it.name ?: "", isDirectory = it.isDirectory)
                }
                return Outcome.Ok(list)
            }

            val list = documentFile.listFiles().flatMap {
                buildList {
                    val file = KMPFileRef(ref = it.uri.toString(), name = it.name ?: "", isDirectory = it.isDirectory)
                    add(file)

                    if (it.isDirectory) {
                        addAll(list(file).unwrapOrNull() ?: emptyList())
                    }
                }
            }

            return Outcome.Ok(list)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val size: Long

            val cursor = context.applicationContext.contentResolver.query(
                ref.ref.toUri(),
                null,
                null,
                null,
                null
            ) ?: return Outcome.Error(FileMetadataException())

            cursor.use {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                it.moveToFirst()
                size = it.getLong(sizeIndex)
            }

            Outcome.Ok(KMPFileMetadata(size = size))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun exists(ref: KMPFileRef): Boolean {
        val context = context ?: return false

        val documentFile = if (ref.isDirectory) {
            DocumentFile.fromTreeUri(context.applicationContext, ref.ref.toUri()) ?: return false
        } else {
            DocumentFile.fromSingleUri(context.applicationContext, ref.ref.toUri()) ?: return false
        }

        return documentFile.exists()
    }
}

@SuppressLint("Recycle")
actual fun KMPFileRef.source(): Outcome<Source, Exception> {
    return try {
        val context = AndroidKMPFileHandler.context ?: return Outcome.Error(NotInitializedException())
        val stream = context.applicationContext.contentResolver.openInputStream(ref.toUri())
            ?: return Outcome.Error(FileOpenException())
        Outcome.Ok(stream.source())
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

@SuppressLint("Recycle")
actual fun KMPFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return try {
        val context = AndroidKMPFileHandler.context ?: return Outcome.Error(NotInitializedException())
        val modeString = when (mode) {
            KMPFileWriteMode.Overwrite -> "wt"
            KMPFileWriteMode.Append -> "wa"
        }

        val outputStream = context.applicationContext.contentResolver.openOutputStream(ref.toUri(), modeString)
            ?: return Outcome.Error(FileCreateException())
        Outcome.Ok(outputStream.sink())
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
