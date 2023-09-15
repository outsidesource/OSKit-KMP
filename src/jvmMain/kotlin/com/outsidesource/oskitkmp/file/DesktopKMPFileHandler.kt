package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.FileMetadata
import okio.Sink
import okio.Source

actual class KMPFileHandlerContext

class DesktopKMPFileHandler : IKMPFileHandler {
    private var context: KMPFileHandlerContext? = null

    override fun init(fileHandlerContext: KMPFileHandlerContext) {
        context = fileHandlerContext
    }

    override suspend fun pickFile(
        startingDir: KMPFileURI?,
        filter: KMPFileFilter?
    ): Outcome<KMPFileURI?, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun pickFolder(startingDir: KMPFileURI?): Outcome<KMPFileURI?, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveFile(
        dir: KMPFileURI,
        name: String,
        mustCreate: Boolean
    ): Outcome<KMPFileURI, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDirectory(
        dir: KMPFileURI,
        name: String,
        mustCreate: Boolean
    ): Outcome<KMPFileURI, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun rename(file: KMPFileURI, name: String): Outcome<KMPFileURI, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(file: KMPFileURI, isRecursive: Boolean): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun list(dir: KMPFileURI, isRecursive: Boolean): Outcome<List<KMPFileURI>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun readMetadata(file: KMPFileURI): Outcome<FileMetadata, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(file: KMPFileURI): Boolean {
        TODO("Not yet implemented")
    }
}

actual fun KMPFileURI.source(): Outcome<Source, Exception> {
    TODO()
}

actual fun KMPFileURI.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    TODO()
}
