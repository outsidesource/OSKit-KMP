package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.use
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

/**
 * The Internal API interface for interacting with sandboxed files/directories
 */
interface IInternalKmpFs : IKmpFs {
    /**
     * The root of the sandboxed filesystem. This ref is always available as a starting point.
     */
    val root: KmpFsRef
}

/**
 * The External API interface for interacting with non-sandboxed files/directories. A ref must be picked for a starting point.
 */
interface IExternalKmpFs : IKmpFs, IKmpFsFilePicker {

    /**
     * Shows a file picker for a user to select a single file.
     *
     * @param startingDir The directory the file picker will start from.
     * @param filter Allows preventing selection of specific file types.
     */
    override suspend fun pickFile(startingDir: KmpFsRef?, filter: KmpFileFilter?): Outcome<KmpFsRef?, KmpFsError>

    /**
     * Shows a file picker for a user to select multiple files.
     *
     * @param startingDir The directory the file picker will start from.
     * @param filter Allows preventing selection of specific file types.
     */
    override suspend fun pickFiles(startingDir: KmpFsRef?, filter: KmpFileFilter?): Outcome<List<KmpFsRef>?, KmpFsError>

    /**
     * Shows a directory picker for a user to select a directory.
     *
     * @param startingDir The directory the file picker will start from.
     */
    override suspend fun pickDirectory(startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError>

    /**
     * Opens a picker for saving a new file. Android and Desktop allow the user to specify the name.
     * iOS does not have a native save dialog and will instead show a directory picker to save the file. The newly
     * created file ref is returned unless the dialog is cancelled.
     *
     * @param fileName The suggested file name to the user. The user can override this.
     * @param startingDir The directory the file picker will start from.
     */
    override suspend fun pickSaveFile(fileName: String, startingDir: KmpFsRef?): Outcome<KmpFsRef?, KmpFsError>

    /**
     * Saves data into a new file. This will present a file picker on all non-JS platforms. On JS platforms
     * this will show a file picker unless the user has downloads automatically saved to a specific folder.
     */
    suspend fun saveFile(bytes: ByteArray, fileName: String): Outcome<Unit, KmpFsError>

    /**
     * [saveFile] Save data from a source into a new file
     *
     * @param source The KmpFsSource to read from
     * @param fileName The suggested file name to the user. The user can override this.
     */
    suspend fun saveFile(source: IKmpIoSource, fileName: String): Outcome<Unit, KmpFsError> =
        saveFile(source.readAll(), fileName)

    /**
     * Attempts to create a KmpFileRef from the provided path string. This is only supported on
     * the JVM due to sandbox constraints on other platforms.
     *
     * @param path A normalized path string. Windows and Unix style paths are both acceptable.
     */
    suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError>
}

interface IKmpFs {
    /**
     * Opens or creates a file (if the create option is provided)
     *
     * @param dir The directory to open/create the file in
     * @param name The file's name
     * @param create Creates the file if it does not exist
     */
    suspend fun resolveFile(dir: KmpFsRef, name: String, create: Boolean = false): Outcome<KmpFsRef, KmpFsError>

    /**
     * Opens or creates a directory (if the create option is provided)
     *
     * @param dir The directory to open/create the directory in
     * @param name The directory's name
     * @param create Creates the directory if it does not exist
     */
    suspend fun resolveDirectory(dir: KmpFsRef, name: String, create: Boolean = false): Outcome<KmpFsRef, KmpFsError>

    /**
     * Deletes a file or directory
     */
    suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError>

    /**
     * Lists the contents of a directory
     *
     * @param dir The directory to list
     * @param isRecursive If true, each directory will be traversed recursively and return all descendants of the parent directory
     */
    suspend fun list(dir: KmpFsRef, isRecursive: Boolean = false): Outcome<List<KmpFsRef>, KmpFsError>

    /**
     * Reads metadata of a ref
     */
    suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError>

    /**
     * Checks if a file or directory exists
     */
    suspend fun exists(ref: KmpFsRef): Boolean

    /**
     * Resolves a file with a path that is not required to be a direct descendant of [dir].
     *
     * @param dir The parent directory
     * @param path The relative path of the file from [dir]. This can be a Windows or Unix path.
     * @param create If true all directory segments will be created if they do not already exist
     *
     * Example:
     * ```
     * resolveNestedFile(KmpFs.Internal.root, "/dir1/dir2/dir3/dir4/test.txt", create = true)
     * ```
     */
    suspend fun resolveFileWithPath(
        dir: KmpFsRef,
        path: String,
        create: Boolean = false,
    ): Outcome<KmpFsRef, KmpFsError> {
        var localDir = dir
        val pathSegments = path.trim('/', '\\').split('/', '\\')
        pathSegments.forEachIndexed { i, segment ->
            if (i == pathSegments.size - 1) return resolveFile(localDir, segment, create)
            localDir = resolveDirectory(localDir, segment, create).unwrapOrReturn { return it }
        }
        return Outcome.Error(KmpFsError.RefNotFound)
    }

    /**
     * Resolves a file with a path that is not required to be a direct descendant of [dir].
     *
     * @param dir The parent directory
     * @param path The relative path of the directory from [dir]. This can be a Windows or Unix path.
     * @param create If true all directory segments will be created if they do not already exist
     *
     * Example:
     * ```
     * resolveNestedDirectory(KmpFs.Internal.root, "/dir1/dir2/dir3/dir4/dir5", create = true)
     * ```
     */
    suspend fun resolveDirectoryWithPath(
        dir: KmpFsRef,
        path: String,
        create: Boolean = false,
    ): Outcome<KmpFsRef, KmpFsError> {
        var localDir = dir
        val pathSegments = path.trim('/', '\\').split('/', '\\')
        pathSegments.forEachIndexed { i, segment ->
            if (i == pathSegments.size - 1) return resolveDirectory(localDir, segment, create)
            localDir = resolveDirectory(localDir, segment, create).unwrapOrReturn { return it }
        }
        return Outcome.Error(KmpFsError.RefNotFound)
    }

    /**
     * Lists a directories files but also includes their relative depth to [dir]
     */
    suspend fun listWithDepth(dir: KmpFsRef): Outcome<List<KmpFsRefListItem>, KmpFsError> = listWithDepth(0, dir)

    private suspend fun listWithDepth(depth: Int, ref: KmpFsRef): Outcome<List<KmpFsRefListItem>, KmpFsError> {
        val refs = buildList {
            list(ref, isRecursive = false)
                .unwrapOrReturn { return it }
                .forEach {
                    add(KmpFsRefListItem(depth, it))
                    if (!it.isDirectory) return@forEach
                    addAll(listWithDepth(depth + 1, it).unwrapOrReturn { return it })
                }
        }

        return Outcome.Ok(refs)
    }

    /**
     * Moves a file to another destination. The destination file must exist and will be overwritten.
     */
    suspend fun moveFile(from: KmpFsRef, to: KmpFsRef): Outcome<Unit, KmpFsError> {
        val source = from.source().unwrapOrReturn { return it }
        val sink = to.sink().unwrapOrReturn { return it }

        try {
            sink.use { it.writeAll(source) }
        } catch (t: KmpFsError) {
            return Outcome.Error(t)
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }

        delete(from).unwrapOrReturn { return it }

        return Outcome.Ok(Unit)
    }

    /**
     * Copies a file to another destination.
     */
    suspend fun copyFile(from: KmpFsRef, to: KmpFsRef): Outcome<Unit, KmpFsError> {
        val source = from.source().unwrapOrReturn { return it }
        val sink = to.sink().unwrapOrReturn { return it }

        try {
            sink.use { it.writeAll(source) }
        } catch (t: KmpFsError) {
            return Outcome.Error(t)
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }

        return Outcome.Ok(Unit)
    }
}

interface IKmpFsFilePicker {
    suspend fun pickFile(startingDir: KmpFsRef? = null, filter: KmpFileFilter? = null): Outcome<KmpFsRef?, KmpFsError>
    suspend fun pickFiles(startingDir: KmpFsRef? = null, filter: KmpFileFilter? = null): Outcome<List<KmpFsRef>?, KmpFsError>
    suspend fun pickDirectory(startingDir: KmpFsRef? = null): Outcome<KmpFsRef?, KmpFsError>
    suspend fun pickSaveFile(fileName: String, startingDir: KmpFsRef? = null): Outcome<KmpFsRef?, KmpFsError>
}

/**
 * Returned from [IKmpFs.listWithDepth]
 *
 * @property depth The depth relative to the starting directory
 * @property ref The ref of the item
 */
data class KmpFsRefListItem(val depth: Int, val ref: KmpFsRef)
