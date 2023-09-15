package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileMetadata

expect class KMPFileHandlerContext

// TODO: Test removing permissions after giving access and then trying to open file
// TODO: Allow Multiple selection of files/folders

/**
 * Provides multiplatform filesystem interactions for content outside of application sandboxes in iOS and Android.
 * All files/folders created are user accessible.
 *
 * In order to access any file a user must call [pickFolder] to gain permissions to a root folder. The user may then
 * take any action within that folder.
 */
interface IKMPFileHandler {
    fun init(fileHandlerContext: KMPFileHandlerContext)
    suspend fun pickFile(
        startingDir: KMPFile? = null,
        filter: KMPFileFilter? = null
    ): Outcome<KMPFile?, Exception>
    suspend fun pickFolder(startingDir: KMPFile? = null): Outcome<KMPFile?, Exception>
    suspend fun openFile(dir: KMPFile, name: String, mustCreate: Boolean = false): Outcome<KMPFile, Exception>
    suspend fun createDirectory(dir: KMPFile, name: String): Outcome<KMPFile, Exception>
    suspend fun rename(file: KMPFile, name: String): Outcome<KMPFile, Exception>
    suspend fun delete(file: KMPFile, isRecursive: Boolean = false): Outcome<Unit, Exception>
    suspend fun list(dir: KMPFile, isRecursive: Boolean = false): Outcome<List<KMPFile>, Exception>
    suspend fun readMetadata(file: KMPFile): Outcome<FileMetadata, Exception>
    suspend fun exists(file: KMPFile): Boolean

    /**
     * Convenience functions
     */
    suspend fun openFile(
        dir: KMPFile,
        segments: List<String>,
        mustCreate: Boolean = false
    ): Outcome<KMPFile, Exception> {
        TODO()
    }

    suspend fun move(from: KMPFile, to: KMPFile): Outcome<Unit, Exception> {
        TODO()
    }

    suspend fun copy(from: KMPFile, to: KMPFile): Outcome<Unit, Exception> {
        TODO()
    }

    suspend fun exists(dir: KMPFile, name: String): Boolean {
        return when (val outcome = openFile(dir, name)) {
            is Outcome.Ok -> return exists(outcome.value)
            is Outcome.Error -> false
        }
    }

    suspend fun readMetadata(dir: KMPFile, name: String): Outcome<FileMetadata, Exception> {
        return when (val outcome = openFile(dir, name)) {
            is Outcome.Ok -> return readMetadata(outcome.value)
            is Outcome.Error -> outcome
        }
    }

    suspend fun delete(dir: KMPFile, name: String): Outcome<Unit, Exception> {
        return when (val outcome = openFile(dir, name)) {
            is Outcome.Ok -> return delete(outcome.value)
            is Outcome.Error -> outcome
        }
    }
}

class NotInitializedException : Exception("KMPFileHandler has not been initialized")
class UnableToOpenFileException : Exception("KMPFileHandler could not open the specified file")

typealias KMPFileFilter = List<KMPFileFilterType>

data class KMPFileFilterType(
    val extension: String,
    val mimeType: String,
)
