package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import okio.buffer
import okio.use

expect class KmpFileHandlerContext

/**
 * Provides limited multiplatform (iOS, Android, and Desktop) filesystem interactions for content outside of
 * application sandboxes in iOS and Android. All files/directories created are user accessible from outside the application.
 *
 * In order to access any file a user must call [pickDirectory] or [pickFile]. Use [pickDirectory] to gain permissions to a
 * root directories. The user may then take any action within that directories.
 *
 * In order to rename files, use [moveFile] command.
 *
 * [resolveFile] and [resolveDirectory] will return a file or directory reference if it exists with an optional
 * parameter to create.
 *
 * WASM:
 * Due to browser constraints, [KmpFileHandler] on WASM only supports a subset of functionality available in other targets.
 *  * Reading files in WASM loads the entire file contents into memory when using `KmpFileRef.sink()`
 *  * All `startingDirectory` parameters are ignored
 *  * Persisting file/directory references is not supported
 *  * Only Chrome and its derivatives support all other features
 *  * Unsupported features:
 *     * Persistable strings
 *
 * Desktop/JVM:
 * Using KmpFileHandler for the desktop/JVM target will require consumers to include LWJGL's tinyfd library in their classpath
 * ```
 * val lwjglVersion = "3.3.3"
 * val lwjglNatives = Pair(
 * 	System.getProperty("os.name")!!,
 * 	System.getProperty("os.arch")!!
 * ).let { (name, arch) ->
 * 	when {
 * 		arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
 * 			if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
 * 				"natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
 * 			else if (arch.startsWith("ppc"))
 * 				"natives-linux-ppc64le"
 * 			else if (arch.startsWith("riscv"))
 * 				"natives-linux-riscv64"
 * 			else
 * 				"natives-linux"
 * 		arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }     ->
 * 			"natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
 * 		arrayOf("Windows").any { name.startsWith(it) }                ->
 * 			if (arch.contains("64"))
 * 				"natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
 * 			else
 * 				"natives-windows-x86"
 * 		else                                                                            ->
 * 			throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
 * 	}
 * }
 *
 * dependencies {
 *     runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
 *     runtimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:$lwjglNatives")
 * }
 * ```
 *
 * It may also be necessary to add the `jdk.unsupported` module for some linux builds:
 * ```
 * compose.desktop {
 *    nativeDistributions {
 *        modules("jdk.unsupported")
 *    }
 * }
 * ```
 */
expect class KmpFileHandler() : IKmpFileHandler {
    override fun init(fileHandlerContext: KmpFileHandlerContext)
    override suspend fun pickFile(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception>

    override suspend fun pickFiles(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFileRef>?, Exception>

    override suspend fun pickDirectory(startingDir: KmpFileRef?): Outcome<KmpFileRef?, Exception>
    override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFileRef?,
    ): Outcome<KmpFileRef?, Exception>

    override suspend fun resolveFile(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception>

    override suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception>
    override suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception>

    override suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception>
    override suspend fun list(
        dir: KmpFileRef,
        isRecursive: Boolean,
    ): Outcome<List<KmpFileRef>, Exception>

    override suspend fun readMetadata(ref: KmpFileRef): Outcome<KmpFileMetadata, Exception>
    override suspend fun exists(ref: KmpFileRef): Boolean
}

interface IKmpFileHandler {
    fun init(fileHandlerContext: KmpFileHandlerContext)

    suspend fun pickFile(
        startingDir: KmpFileRef? = null,
        filter: KmpFileFilter? = null,
    ): Outcome<KmpFileRef?, Exception>

    suspend fun pickFiles(
        startingDir: KmpFileRef? = null,
        filter: KmpFileFilter? = null,
    ): Outcome<List<KmpFileRef>?, Exception>

    suspend fun pickDirectory(startingDir: KmpFileRef? = null): Outcome<KmpFileRef?, Exception>

    /**
     * [pickSaveFile] opens a picker for saving a new file. Android and Desktop allow the user to specify the name.
     * iOS does not have a native save dialog and will instead show a directory picker to save the file. The newly
     * created file ref is returned unless the dialog is cancelled.
     */
    suspend fun pickSaveFile(fileName: String, startingDir: KmpFileRef? = null): Outcome<KmpFileRef?, Exception>

    /**
     * [create] Creates the file if it does not exist
     */
    suspend fun resolveFile(dir: KmpFileRef, name: String, create: Boolean = false): Outcome<KmpFileRef, Exception>

    /**
     * [resolveRefFromPath] Attempts to create a KMPFileRef from the provided path string. This is not guaranteed to
     * work and will most likely fail on Android and iOS due to paths not being properly sandboxed. This method
     * exists primarily for desktop where sandboxes are not an issue. Android should use a Uri string for the path.
     */
    suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception>

    /**
     * [create] Creates the directory if it does not exist
     */
    suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean = false,
    ): Outcome<KmpFileRef, Exception>

    suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception>
    suspend fun list(dir: KmpFileRef, isRecursive: Boolean = false): Outcome<List<KmpFileRef>, Exception>
    suspend fun readMetadata(ref: KmpFileRef): Outcome<KmpFileMetadata, Exception>
    suspend fun exists(ref: KmpFileRef): Boolean

    /**
     * Convenience functions
     */

    /**
     * [moveFile] moves a file to another destination. The destination file must exist and will be overwritten.
     */
    suspend fun moveFile(from: KmpFileRef, to: KmpFileRef): Outcome<Unit, Exception> {
        if (from.isDirectory || to.isDirectory) return Outcome.Error(FileMoveError())
        val source = from.source().unwrapOrReturn { return this }
        val sink = to.sink().unwrapOrReturn { return this }

        try {
            sink.buffer().use { it.writeAll(source) }
        } catch (e: Exception) {
            return Outcome.Error(e)
        }

        delete(from).unwrapOrReturn { return this }

        return Outcome.Ok(Unit)
    }

    suspend fun copyFile(from: KmpFileRef, to: KmpFileRef): Outcome<Unit, Exception> {
        if (from.isDirectory || to.isDirectory) return Outcome.Error(FileCopyError())
        val source = from.source().unwrapOrReturn { return this }
        val sink = to.sink().unwrapOrReturn { return this }

        try {
            sink.buffer().use { it.writeAll(source) }
        } catch (e: Exception) {
            return Outcome.Error(e)
        }

        return Outcome.Ok(Unit)
    }

    suspend fun exists(dir: KmpFileRef, name: String): Boolean {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return exists(outcome.value)
            is Outcome.Error -> false
        }
    }

    suspend fun readMetadata(dir: KmpFileRef, name: String): Outcome<KmpFileMetadata, Exception> {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return readMetadata(outcome.value)
            is Outcome.Error -> outcome
        }
    }

    suspend fun delete(dir: KmpFileRef, name: String): Outcome<Unit, Exception> {
        return when (val outcome = resolveFile(dir, name)) {
            is Outcome.Ok -> return delete(outcome.value)
            is Outcome.Error -> outcome
        }
    }
}

typealias KmpFileFilter = List<KmpFileMimetype>

/**
 * [extension] defines the file extension used i.e. "txt", "png", "jpg"
 * [mimeType] defined the mimetype used i.e. "text/plain", "image/png", "image/jpeg"
 */
data class KmpFileMimetype(
    val extension: String,
    val mimeType: String,
)

data class KmpFileMetadata(
    val size: Long,
)

class RefIsDirectoryReadWriteError : Exception("Cannot read/write from ref. It is a directory")
class NotInitializedError : Exception("KmpFileHandler has not been initialized")
class FileOpenError : Exception("KmpFileHandler could not open the specified file")
class FileCreateError : Exception("KmpFileHandler could not create the specified file")
class FileDeleteError : Exception("KmpFileHandler could not delete the specified file")
class FileNotFoundError : Exception("KmpFileHandler could not find the specified file")
class FileMetadataError : Exception("KmpFileHandler could not fetch metadata for the specified file")
class FileListError : Exception("KmpFileHandler could not list directory contents for the specified directory")
class FileMoveError : Exception("KmpFileHandler could not move the specified file")
class FileCopyError : Exception("KmpFileHandler could not copy the specified file")
class NotSupportedError : Exception("KmpFileHandler does not support this operation on this platform")
