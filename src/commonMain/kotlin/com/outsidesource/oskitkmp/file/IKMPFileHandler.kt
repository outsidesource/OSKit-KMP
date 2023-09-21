package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrElse
import okio.buffer
import okio.use

expect class KMPFileHandlerContext

/**
 * Provides limited multiplatform filesystem interactions for content outside of application sandboxes in
 * iOS and Android. All files/folders created are user accessible from outside the application.
 *
 * In order to access any file a user must call [pickFolder] or [pickFile]. Use [pickFolder] to gain permissions to a
 * root folder. The user may then take any action within that folder.
 */
interface IKMPFileHandler {
    fun init(fileHandlerContext: KMPFileHandlerContext)

    suspend fun pickFile(
        startingDir: KMPFileRef? = null,
        filter: KMPFileFilter? = null
    ): Outcome<KMPFileRef?, Exception>

    suspend fun pickFolder(startingDir: KMPFileRef? = null): Outcome<KMPFileRef?, Exception>

    suspend fun pickSaveFile(defaultName: String? = null): Outcome<KMPFileRef?, Exception>

    /**
     * [create] Creates the file if it does not exist
     */
    suspend fun resolveFile(dir: KMPFileRef, name: String, create: Boolean = false): Outcome<KMPFileRef, Exception>

    /**
     * [create] Creates the directory if it does not exist
     */
    suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean = false
    ): Outcome<KMPFileRef, Exception>

    /**
     * Renames a file or folder.
     *
     * Android does not support renaming of files. A similar effect can be achieved by using [move] with [pickSaveFile].
     */
    suspend fun rename(ref: KMPFileRef, name: String): Outcome<KMPFileRef, Exception>
    suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception>
    suspend fun list(dir: KMPFileRef, isRecursive: Boolean = false): Outcome<List<KMPFileRef>, Exception>
    suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception>
    suspend fun exists(ref: KMPFileRef): Boolean

    /**
     * Convenience functions
     */
    suspend fun resolveFile(
        dir: KMPFileRef,
        segments: List<String>,
        create: Boolean = false
    ): Outcome<KMPFileRef, Exception> {
        TODO()
    }

    suspend fun resolveDirectory(
        dir: KMPFileRef,
        segments: List<String>,
        create: Boolean = false
    ): Outcome<KMPFileRef, Exception> {
        TODO()
    }

    suspend fun move(from: KMPFileRef, to: KMPFileRef): Outcome<Unit, Exception> {
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

    suspend fun copy(from: KMPFileRef, to: KMPFileRef): Outcome<Unit, Exception> {
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
}

typealias KMPFileFilter = List<KMPFileMimeType>

data class KMPFileMimeType(
    val extension: String,
    val mimeType: String,
)

data class KMPFileMetadata(
    val size: Long,
)

class NotInitializedException : Exception("KMPFileHandler has not been initialized")
class FileOpenException : Exception("KMPFileHandler could not open the specified file")
class FileCreateException : Exception("KMPFileHandler could not create the specified file")
class FileRenameException : Exception("KMPFileHandler could not rename the specified file")
class FileDeleteException : Exception("KMPFileHandler could not delete the specified file")
class FileNotFoundException : Exception("KMPFileHandler could not find the specified file")
class FileMetadataException : Exception("KMPFileHandler could not fetch metadata for the specified file")
