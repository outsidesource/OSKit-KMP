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
        startingDir: KMPFile?,
        filter: KMPFileFilter?
    ): Outcome<KMPFile?, Exception> {
        return try {
            val context = context ?: return Outcome.Error(NotInitializedException())
            val fileResultLauncher = openFileResultLauncher ?: return Outcome.Error(NotInitializedException())

            fileResultLauncher.launch(filter?.map { it.mimeType }?.toTypedArray() ?: arrayOf("*/*"))
            val uri = openFileResultFlow.firstOrNull() ?: return Outcome.Ok(null)

            val name = DocumentFile.fromSingleUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(UnableToOpenFileException())

            Outcome.Ok(KMPFile(path = uri.path.toString(), isDirectory = false, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    override suspend fun pickFolder(startingDir: KMPFile?): Outcome<KMPFile?, Exception> {
        return try {
            val folderResultLauncher = openFolderResultLauncher ?: return Outcome.Error(NotInitializedException())
            val context = context ?: return Outcome.Error(NotInitializedException())

            folderResultLauncher.launch(startingDir?.path?.toUri())
            val uri = openFolderResultFlow.firstOrNull() ?: return Outcome.Ok(null)
            val name = DocumentFile.fromTreeUri(context.applicationContext, uri)?.name
                ?: return Outcome.Error(UnableToOpenFileException())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            Outcome.Ok(KMPFile(path = uri.toString(), isDirectory = true, name = name))
        } catch (e: Exception) {
            Outcome.Error(e)
        }
    }

    /**
     * Note: ACTION_CREATE_DOCUMENT cannot overwrite an existing file. If your app tries to save a file with the
     * same name, the system appends a number in parentheses at the end of the file name.
     *
     * For example, if your app tries to save a file called confirmation.pdf in a directory that already has a file
     * with that name, the system saves the new file with the name confirmation(1).pdf.
     */
    override suspend fun openFile(dir: KMPFile, name: String, mustCreate: Boolean): Outcome<KMPFile, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(dir: KMPFile, name: String): Outcome<KMPFile, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun rename(file: KMPFile, name: String): Outcome<KMPFile, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(file: KMPFile, isRecursive: Boolean): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun list(dir: KMPFile, isRecursive: Boolean): Outcome<List<KMPFile>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun readMetadata(file: KMPFile): Outcome<FileMetadata, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(file: KMPFile): Boolean {
        TODO("Not yet implemented")
    }
}

actual fun KMPFile.source(): Outcome<Source, Exception> {
    return try {
        Outcome.Ok(fileSystem.source(path.toPath()))
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}

actual fun KMPFile.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return try {
        val sink = when (mode) {
            KMPFileWriteMode.Append -> fileSystem.appendingSink(path.toPath(), mustExist = true)
            KMPFileWriteMode.Overwrite -> fileSystem.sink(path.toPath(), mustCreate = false)
        }
        Outcome.Ok(sink)
    } catch (e: Exception) {
        Outcome.Error(e)
    }
}
