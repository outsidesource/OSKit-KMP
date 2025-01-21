package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.use
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

interface IInternalKmpFs : IKmpFs {
    val root: KmpFsRef
}

interface IExternalKmpFs : IKmpFs {
    suspend fun pickFile(
        startingDir: KmpFsRef? = null,
        filter: KmpFileFilter? = null,
    ): Outcome<KmpFsRef?, KmpFsError>

    suspend fun pickFiles(
        startingDir: KmpFsRef? = null,
        filter: KmpFileFilter? = null,
    ): Outcome<List<KmpFsRef>?, KmpFsError>

    suspend fun pickDirectory(startingDir: KmpFsRef? = null): Outcome<KmpFsRef?, KmpFsError>

    /**
     * [pickSaveFile] opens a picker for saving a new file. Android and Desktop allow the user to specify the name.
     * iOS does not have a native save dialog and will instead show a directory picker to save the file. The newly
     * created file ref is returned unless the dialog is cancelled.
     */
    suspend fun pickSaveFile(fileName: String, startingDir: KmpFsRef? = null): Outcome<KmpFsRef?, KmpFsError>

    /**
     * [saveFile] Saves data into a new file. This will present a file picker on all non-JS platforms. On JS platforms
     * this will show a file picker unless the user has downloads automatically saved to a specific folder.
     */
    suspend fun saveFile(bytes: ByteArray, fileName: String): Outcome<Unit, KmpFsError>

    /**
     * [saveFile] Save data from a source into a new file
     * @param source The KmpFsSource to read from
     */
    suspend fun saveFile(source: IKmpIoSource, fileName: String): Outcome<Unit, KmpFsError> =
        saveFile(source.readRemaining(), fileName)
}

interface IKmpFs {
    /**
     * [create] Creates the file if it does not exist
     */
    suspend fun resolveFile(dir: KmpFsRef, fileName: String, create: Boolean = false): Outcome<KmpFsRef, KmpFsError>
    suspend fun resolveDirectory(dir: KmpFsRef, name: String, create: Boolean = false): Outcome<KmpFsRef, KmpFsError>

    /**
     * [resolveRefFromPath] Attempts to create a KmpFileRef from the provided path string. This is not guaranteed to
     * work and will most likely fail on Android and iOS due to paths not being properly sandboxed. This method
     * exists primarily for desktop where sandboxes are not an issue. Android should use a Uri string for the path.
     */
    suspend fun resolveRefFromPath(path: String): Outcome<KmpFsRef, KmpFsError>

    suspend fun delete(ref: KmpFsRef): Outcome<Unit, KmpFsError>
    suspend fun list(dir: KmpFsRef, isRecursive: Boolean = false): Outcome<List<KmpFsRef>, KmpFsError>
    suspend fun readMetadata(ref: KmpFsRef): Outcome<KmpFileMetadata, KmpFsError>
    suspend fun exists(ref: KmpFsRef): Boolean

    /**
     * Convenience functions
     */

    /**
     * [moveFile] moves a file to another destination. The destination file must exist and will be overwritten.
     */
    suspend fun moveFile(from: KmpFsRef, to: KmpFsRef): Outcome<Unit, KmpFsError> {
        if (from.isDirectory || to.isDirectory) return Outcome.Error(KmpFsError.MoveError)
        val source = from.source().unwrapOrReturn { return it }
        val sink = to.sink().unwrapOrReturn { return it }

        try {
            sink.use { it.writeAll(source) }
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }

        delete(from).unwrapOrReturn { return it }

        return Outcome.Ok(Unit)
    }

    suspend fun copyFile(from: KmpFsRef, to: KmpFsRef): Outcome<Unit, KmpFsError> {
        if (from.isDirectory || to.isDirectory) return Outcome.Error(KmpFsError.CopyError)
        val source = from.source().unwrapOrReturn { return it }
        val sink = to.sink().unwrapOrReturn { return it }

        try {
            sink.use { it.writeAll(source) }
        } catch (t: Throwable) {
            return Outcome.Error(KmpFsError.Unknown(t))
        }

        return Outcome.Ok(Unit)
    }

    suspend fun exists(dir: KmpFsRef, fileName: String): Boolean {
        return when (val outcome = resolveFile(dir, fileName)) {
            is Outcome.Ok -> return exists(outcome.value)
            is Outcome.Error -> false
        }
    }

    suspend fun readMetadata(dir: KmpFsRef, fileName: String): Outcome<KmpFileMetadata, KmpFsError> {
        return when (val outcome = resolveFile(dir, fileName)) {
            is Outcome.Ok -> return readMetadata(outcome.value)
            is Outcome.Error -> outcome
        }
    }

    suspend fun delete(dir: KmpFsRef, fileName: String): Outcome<Unit, KmpFsError> {
        return when (val outcome = resolveFile(dir, fileName)) {
            is Outcome.Ok -> return delete(outcome.value)
            is Outcome.Error -> outcome
        }
    }
}
