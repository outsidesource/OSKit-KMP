package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Path
import okio.Sink
import okio.Source

actual class KmpFileHandlerContext()

actual class KmpFileHandler : IKmpFileHandler {
    private var context: KmpFileHandlerContext? = null
    private val pathSeparatorChars = Path.DIRECTORY_SEPARATOR.toCharArray()

    actual override fun init(fileHandlerContext: KmpFileHandlerContext) {
        context = fileHandlerContext
    }

    actual override suspend fun pickFile(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<KmpFileRef?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun pickFiles(
        startingDir: KmpFileRef?,
        filter: KmpFileFilter?,
    ): Outcome<List<KmpFileRef>?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun pickDirectory(startingDir: KmpFileRef?): Outcome<KmpFileRef?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KmpFileRef?,
    ): Outcome<KmpFileRef?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun resolveFile(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun resolveDirectory(
        dir: KmpFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KmpFileRef, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KmpFileRef, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun delete(ref: KmpFileRef): Outcome<Unit, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun list(dir: KmpFileRef, isRecursive: Boolean): Outcome<List<KmpFileRef>, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun readMetadata(ref: KmpFileRef): Outcome<KMPFileMetadata, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun exists(ref: KmpFileRef): Boolean {
        return false
    }
}

actual fun KmpFileRef.source(): Outcome<Source, Exception> {
    return Outcome.Error(Exception("Not supported"))
}

actual fun KmpFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return Outcome.Error(Exception("Not supported"))
}
