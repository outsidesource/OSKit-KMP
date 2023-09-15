package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome

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
        startingDir: KMPFileURI? = null,
        filter: KMPFileFilter? = null
    ): Outcome<KMPFileURI?, Exception>
    suspend fun pickFolder(startingDir: KMPFileURI? = null): Outcome<KMPFileURI?, Exception>

    /**
     * [mustCreate] Creates the file if it does not exist
     */
    suspend fun resolveFile(dir: KMPFileURI, name: String, mustCreate: Boolean = false): Outcome<KMPFileURI, Exception>

    /**
     * [mustCreate] Creates the directory if it does not exist
     */
    suspend fun resolveDirectory(
        dir: KMPFileURI,
        name: String,
        mustCreate: Boolean = false
    ): Outcome<KMPFileURI, Exception>
    suspend fun rename(file: KMPFileURI, name: String): Outcome<KMPFileURI, Exception>
    suspend fun delete(file: KMPFileURI, isRecursive: Boolean = false): Outcome<Unit, Exception>
    suspend fun list(dir: KMPFileURI, isRecursive: Boolean = false): Outcome<List<KMPFileURI>, Exception>
    suspend fun readMetadata(file: KMPFileURI): Outcome<KMPFileMetadata, Exception>
    suspend fun exists(file: KMPFileURI): Boolean

    /**
     * Convenience functions
     */
    suspend fun resolveFile(
        dir: KMPFileURI,
        segments: List<String>,
        mustCreate: Boolean = false
    ): Outcome<KMPFileURI, Exception> {
        TODO()
    }

    suspend fun resolveDirectory(
        dir: KMPFileURI,
        segments: List<String>,
        mustCreate: Boolean = false
    ): Outcome<KMPFileURI, Exception> {
        TODO()
    }

    suspend fun move(from: KMPFileURI, to: KMPFileURI): Outcome<Unit, Exception> {
        TODO()
    }

    suspend fun copy(from: KMPFileURI, to: KMPFileURI): Outcome<Unit, Exception> {
        TODO()
    }

    suspend fun exists(dir: KMPFileURI, name: String): Boolean {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return exists(outcome.value)
            is Outcome.Error -> false
        }
    }

    suspend fun readMetadata(dir: KMPFileURI, name: String): Outcome<KMPFileMetadata, Exception> {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return readMetadata(outcome.value)
            is Outcome.Error -> outcome
        }
    }

    suspend fun delete(dir: KMPFileURI, name: String): Outcome<Unit, Exception> {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return delete(outcome.value)
            is Outcome.Error -> outcome
        }
    }
}

typealias KMPFileFilter = List<KMPFileFilterType>

data class KMPFileFilterType(
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
