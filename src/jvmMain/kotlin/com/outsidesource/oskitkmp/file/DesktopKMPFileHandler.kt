package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Sink
import okio.Source

actual class KMPFileHandlerContext

class DesktopKMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KMPFileRef?,
        filter: KMPFileFilter?
    ): Outcome<KMPFileRef?, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun pickSaveFile(defaultName: String?): Outcome<KMPFileRef?, Exception> {
        TODO()
    }

    override suspend fun pickFolder(startingDir: KMPFileRef?): Outcome<KMPFileRef?, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveFile(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDirectory(
        dir: KMPFileRef,
        name: String,
        create: Boolean
    ): Outcome<KMPFileRef, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun rename(ref: KMPFileRef, name: String): Outcome<KMPFileRef, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(ref: KMPFileRef): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun list(dir: KMPFileRef, isRecursive: Boolean): Outcome<List<KMPFileRef>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun readMetadata(ref: KMPFileRef): Outcome<KMPFileMetadata, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(ref: KMPFileRef): Boolean {
        TODO("Not yet implemented")
    }
}

actual fun KMPFileRef.source(): Outcome<Source, Exception> {
    TODO()
}

actual fun KMPFileRef.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    TODO()
}
