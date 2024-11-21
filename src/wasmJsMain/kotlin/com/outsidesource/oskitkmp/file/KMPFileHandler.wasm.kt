package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Path
import okio.Sink
import okio.Source

actual class KMPFileHandlerContext()

actual class KMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null
    private val pathSeparatorChars = Path.DIRECTORY_SEPARATOR.toCharArray()

    actual override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext
    }

    actual override suspend fun pickFile(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?,
    ): Outcome<KMPFileRef?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun pickFiles(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?,
    ): Outcome<List<KMPFileRef>?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun pickDirectory(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun pickSaveFile(
        fileName: String,
        startingDir: KMPFileRef?,
    ): Outcome<KMPFileRef?, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun resolveFile(
        dir: KMPFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KMPFileRef, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean,
    ): Outcome<KMPFileRef, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun resolveRefFromPath(path: String): Outcome<KMPFileRef, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun list(dir: KMPFileRef, isRecursive: Boolean): Outcome<List<KMPFileRef>, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception> {
        return Outcome.Error(Exception("Not supported"))
    }

    actual override suspend fun exists(ref: KMPFileRef): Boolean {
        return false
    }
}

actual fun KMPFileRef.source(): Outcome<Source, Exception> {
    return Outcome.Error(Exception("Not supported"))
}

actual fun KMPFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    return Outcome.Error(Exception("Not supported"))
}
