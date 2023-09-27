package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrElse
import okio.buffer
import okio.use

expect class KMPFileHandlerContext

/**
 * Provides limited multiplatform (iOS, Android, and Desktop) filesystem interactions for content outside of
 * application sandboxes in iOS and Android. All files/folders created are user accessible from outside the application.
 *
 * In order to access any file a user must call [pickDirectory] or [pickFile]. Use [pickDirectory] to gain permissions to a
 * root folder. The user may then take any action within that folder.
 *
 * In order to rename files, use [moveFile] command.
 *
 * [resolveFile] and [resolveDirectory] will return a file or directory reference if it exists with an optional
 * parameter to create.
 */
interface IKMPFileHandler {
    fun init(fileHandlerContext: KMPFileHandlerContext)

    suspend fun pickFile(
        startingDir: KMPFileRef? = null,
        filter: KMPFileFilter? = null
    ): Outcome<KMPFileRef?, Exception>

    suspend fun pickDirectory(startingDir: KMPFileRef? = null): Outcome<KMPFileRef?, Exception>

    /**
     * [pickSaveFile] opens a picker for saving a new file. Android and Desktop allow the user to specify the name.
     * iOS does not have a native save dialog and will instead show a directory picker to save the file. The newly
     * created file ref is returned unless the dialog is cancelled.
     */
    suspend fun pickSaveFile(fileName: String, startingDir: KMPFileRef? = null): Outcome<KMPFileRef?, Exception>

    /**
     * [create] Creates the file if it does not exist
     */
    suspend fun resolveFile(dir: KMPFileRef, name: String, create: Boolean = false): Outcome<KMPFileRef, Exception>

    /**
     * [resolveRefFromPath] Attempts to create a KMPFileRef from the provided path string. This is not guaranteed to
     * work and will most likely fail on Android and iOS due to paths not being properly sandboxed. This method
     * exists primarily for desktop where sandboxes are not an issue. Android should use a Uri string for the path.
     */
    suspend fun resolveRefFromPath(path: String): Outcome<KMPFileRef, Exception>

    /**
     * [create] Creates the directory if it does not exist
     */
    suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean = false
    ): Outcome<KMPFileRef, Exception>

    suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception>
    suspend fun list(dir: KMPFileRef, isRecursive: Boolean = false): Outcome<List<KMPFileRef>, Exception>
    suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception>
    suspend fun exists(ref: KMPFileRef): Boolean

    /**
     * Convenience functions
     */

    /**
     * [moveFile] moves a file to another destination. The destination file must exist and will be overwritten.
     */
    suspend fun moveFile(from: KMPFileRef, to: KMPFileRef): Outcome<Unit, Exception> {
        if (from.isDirectory || to.isDirectory) return Outcome.Error(FileMoveException())
        val source = from.source().unwrapOrElse { return this }
        val sink = to.sink().unwrapOrElse { return this }

        try {
            sink.buffer().use { it.writeAll(source) }
        } catch (e: Exception) {
            return Outcome.Error(e)
        }

        delete(from).unwrapOrElse { return this }

        return Outcome.Ok(Unit)
    }

    suspend fun copyFile(from: KMPFileRef, to: KMPFileRef): Outcome<Unit, Exception> {
        if (from.isDirectory || to.isDirectory) return Outcome.Error(FileCopyException())
        val source = from.source().unwrapOrElse { return this }
        val sink = to.sink().unwrapOrElse { return this }

        try {
            sink.buffer().use { it.writeAll(source) }
        } catch (e: Exception) {
            return Outcome.Error(e)
        }

        return Outcome.Ok(Unit)
    }

    suspend fun exists(dir: KMPFileRef, name: String): Boolean {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return exists(outcome.value)
            is Outcome.Error -> false
        }
    }

    suspend fun readMetadata(dir: KMPFileRef, name: String): Outcome<KMPFileMetadata, Exception> {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return readMetadata(outcome.value)
            is Outcome.Error -> outcome
        }
    }

    suspend fun delete(dir: KMPFileRef, name: String): Outcome<Unit, Exception> {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return delete(outcome.value)
            is Outcome.Error -> outcome
        }
    }

    /**
     * Roadmap
     *
     * 1. renameDirectory
     * 2. Multiple file selection
     * 3. hasAccess() to show if access to the ref was lost due to permission change
     * 4. resolve with file segments for deeply nested file ref resolution
     */

    /**
     * It is difficult to create a common API for Moving/renaming a directory. Each platform handles things differently.
     * iOS can move an item to anywhere as long as permission is granted for the destination URL meaning the user
     * needs a valid KMPFileRef for both the source and the destination. However, the destination directory cannot exist
     * or iOS will cancel the move operation.
     * Android can only rename directories and cannot move them.
     * The current API is hierarchy agnostic so there is no way to make this a simple rename function because the API
     * can't determine if the src and dst are siblings.
     *
     * This functionality could be manually achieved by recursively listing and moving files. However, it would
     * perform poorly because each file is copied individually byte-for-byte instead of a direct filesystem command.
     */
//    suspend fun renameDirectory(from: KMPFileRef, to: KMPFileRef): Outcome<Unit, Exception>

//    suspend fun resolveFile(
//        dir: KMPFileRef,
//        segments: List<String>,
//        create: Boolean = false
//    ): Outcome<KMPFileRef, Exception> {}
//
//    suspend fun resolveDirectory(
//        dir: KMPFileRef,
//        segments: List<String>,
//        create: Boolean = false
//    ): Outcome<KMPFileRef, Exception> {}
}

typealias KMPFileFilter = List<KMPFileMimeType>

/**
 * [extension] defines the file extension used i.e. "txt", "png", "jpg"
 * [mimeType] defined the mimetype used i.e. "text/plain", "image/png", "image/jpeg"
 */
data class KMPFileMimeType(
    val extension: String,
    val mimeType: String,
)

data class KMPFileMetadata(
    val size: Long,
)

class SourceException : Exception("Cannot read from file. It is a directory")
class SinkException : Exception("Cannot write to file. It is a directory")
class NotInitializedException : Exception("KMPFileHandler has not been initialized")
class FileOpenException : Exception("KMPFileHandler could not open the specified file")
class FileCreateException : Exception("KMPFileHandler could not create the specified file")
class FileDeleteException : Exception("KMPFileHandler could not delete the specified file")
class FileNotFoundException : Exception("KMPFileHandler could not find the specified file")
class FileMetadataException : Exception("KMPFileHandler could not fetch metadata for the specified file")
class FileListException : Exception("KMPFileHandler could not list directory contents for the specified directory")
class FileMoveException : Exception("KMPFileHandler could not move the specified file")
class FileCopyException : Exception("KMPFileHandler could not copy the specified file")
