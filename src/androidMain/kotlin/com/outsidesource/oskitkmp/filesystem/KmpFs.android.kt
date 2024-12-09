package com.outsidesource.oskitkmp.filesystem

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
import okio.sink
import okio.source

actual data class KmpFsContext(
    val applicationContext: Context,
    val activity: ComponentActivity,
)

actual class KmpFs : IKmpFs {
    companion object {
        internal var context: KmpFsContext? = null
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var pickFileResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pickFilesResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private val pickFileResultFlow =
        MutableSharedFlow<List<Uri>?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var pickSaveFileResultLauncher: ActivityResultLauncher<String>? = null
    private val pickSaveFileResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var pickFolderResultLauncher: ActivityResultLauncher<Uri?>? = null
    private val pickFolderResultFlow =
        MutableSharedFlow<Uri?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    actual override fun init(fileHandlerContext: KmpFsContext) {
        context = fileHandlerContext

        pickFileResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { data ->
            coroutineScope.launch {
                pickFileResultFlow.emit(if (data != null) listOf(data) else null)
            }
        }

        pickFilesResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { data ->
            coroutineScope.launch {
                pickFileResultFlow.emit(data)
            }
        }

        pickSaveFileResultLauncher = fileHandlerContext.activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("*/*"),
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

    actual override suspend fun pickFile(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFsRef?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val fileResultLauncher = pickFileResultLauncher ?: return Outcome.Error(NotInitializedError())

            fileResultLauncher.launch(filter?.map { it.mimeType }?.toTypedArray() ?: arrayOf("*/*"))
            val uri = pickFileResultFlow.firstOrNull()?.firstOrNull() ?: return Outcome.Ok(null)

            val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenError())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KmpFsRef(ref = uri.toString(), isDirectory = false, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun pickFiles(
        startingDir: KmpFsRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFsRef>?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val filesResultLauncher = pickFilesResultLauncher ?: return Outcome.Error(NotInitializedError())

            filesResultLauncher.launch(filter?.map { it.mimeType }?.toTypedArray() ?: arrayOf("*/*"))
            val uris = pickFileResultFlow.firstOrNull() ?: return Outcome.Ok(null)

            val refs = uris.map { uri ->
                val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                    ?: return Outcome.Error(FileOpenError())

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

                KmpFsRef(ref = uri.toString(), isDirectory = false, name = name)
            }

            Outcome.Ok(refs)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFsRef?,
    ): Outcome<KmpFsRef?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val fileResultLauncher = pickSaveFileResultLauncher ?: return Outcome.Error(NotInitializedError())

            fileResultLauncher.launch(fileName)
            val uri = pickSaveFileResultFlow.firstOrNull() ?: return Outcome.Ok(null)

            val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenError())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KmpFsRef(ref = uri.toString(), isDirectory = false, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, Exception> {
        return try {
            val folderResultLauncher = pickFolderResultLauncher ?: return Outcome.Error(NotInitializedError())
            val context = context ?: return Outcome.Error(NotInitializedError())

            folderResultLauncher.launch(startingDir?.ref?.toUri())
            val uri = pickFolderResultFlow.firstOrNull() ?: return Outcome.Ok(null)
            val name = DocumentFile.fromTreeUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(FileOpenError())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KmpFsRef(ref = uri.toString(), isDirectory = true, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun resolveFile(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val parentUri = DocumentFile.fromTreeUri(context.applicationContext, dir.ref.toUri())
                ?: return Outcome.Error(FileOpenError())
            val file = parentUri.findFile(name)
            if (file == null && !create) return Outcome.Error(FileNotFoundError())
            val createdFile = file ?: parentUri.createFile("", name)
                ?: return Outcome.Error(FileCreateError())

            Outcome.Ok(KmpFsRef(ref = createdFile.uri.toString(), name = name, isDirectory = false))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun resolveDirectory(
        dir: KmpFsRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFsRef, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val parentUri = DocumentFile.fromTreeUri(context.applicationContext, dir.ref.toUri())
                ?: return Outcome.Error(FileOpenError())
            val file = parentUri.findFile(name)
            if (file == null && !create) return Outcome.Error(FileNotFoundError())
            if (file != null && file.isDirectory) return Outcome.Error(FileCreateError())
            val createdDirectory = file ?: parentUri.createDirectory(name)
                ?: return Outcome.Error(FileCreateError())

            Outcome.Ok(KmpFsRef(ref = createdDirectory.uri.toString(), name = name, isDirectory = true))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val file = DocumentFile.fromSingleUri(context.applicationContext, path.toUri())
                ?: return Outcome.Error(FileOpenError())

            Outcome.Ok(KmpFsRef(ref = file.uri.toString(), name = file.name ?: "", isDirectory = file.isDirectory))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun delete(ref: KmpFsRef): Outcome<Unit, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val documentFile = if (ref.isDirectory) {
                DocumentFile.fromTreeUri(context.applicationContext, ref.ref.toUri())
                    ?: return Outcome.Error(FileOpenError())
            } else {
                DocumentFile.fromSingleUri(context.applicationContext, ref.ref.toUri())
                    ?: return Outcome.Error(FileOpenError())
            }

            if (!documentFile.delete()) return Outcome.Error(FileDeleteError())

            return Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun list(dir: KmpFsRef, isRecursive: Boolean): Outcome<List<KmpFsRef>, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            if (!dir.isDirectory) return Outcome.Ok(emptyList())

            val documentFile = DocumentFile.fromTreeUri(context.applicationContext, dir.ref.toUri())
                ?: return Outcome.Error(FileOpenError())

            if (!isRecursive) {
                val list = documentFile.listFiles().map {
                    KmpFsRef(ref = it.uri.toString(), name = it.name ?: "", isDirectory = it.isDirectory)
                }
                return Outcome.Ok(list)
            }

            val list = documentFile.listFiles().flatMap {
                buildList {
                    val file = KmpFsRef(ref = it.uri.toString(), name = it.name ?: "", isDirectory = it.isDirectory)
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

    actual override suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedError())
            val size: Long

            val cursor = context.applicationContext.contentResolver.query(
                ref.ref.toUri(),
                null,
                null,
                null,
                null,
            ) ?: return Outcome.Error(FileMetadataError())

            cursor.use {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                it.moveToFirst()
                size = it.getLong(sizeIndex)
            }

            Outcome.Ok(KmpFileMetadata(size = size))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    actual override suspend fun exists(ref: KmpFsRef): Boolean {
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
actual suspend fun KmpFsRef.source(): Outcome<IKmpFsSource, Exception> {
    return try {
        if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
        val context = KmpFs.context ?: return Outcome.Error(NotInitializedError())
        val stream = context.applicationContext.contentResolver.openInputStream(ref.toUri())
            ?: return Outcome.Error(FileOpenError())
        val source = stream.source()
        Outcome.Ok(OkIoKmpFsSource(source))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

@SuppressLint("Recycle")
actual suspend fun KmpFsRef.sink(mode: KmpFileWriteMode): Outcome<IKmpFsSink, Exception> {
    return try {
        if (isDirectory) return Outcome.Error(RefIsDirectoryReadWriteError())
        val context = KmpFs.context ?: return Outcome.Error(NotInitializedError())
        val modeString = when (mode) {
            KmpFileWriteMode.Overwrite -> "wt"
            KmpFileWriteMode.Append -> "wa"
        }

        val outputStream = context.applicationContext.contentResolver.openOutputStream(ref.toUri(), modeString)
            ?: return Outcome.Error(FileCreateError())
        val sink = outputStream.sink()
        Outcome.Ok(OkIoKmpFsSink(sink))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
