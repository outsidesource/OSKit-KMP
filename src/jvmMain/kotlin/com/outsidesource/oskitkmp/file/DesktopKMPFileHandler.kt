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
        startingDir: KMPFile?,
        filter: KMPFileFilter?
    ): Outcome<KMPFile?, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun pickFolder(startingDir: KMPFile?): Outcome<KMPFile?, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun openFile(dir: KMPFile, name: String, mustCreate: Boolean): Outcome<KMPFile, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(dir: KMPFile, name: String): Outcome<KMPFile, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun rename(file: KMPFile, name: String): Outcome<KMPFile, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(file: KMPFile, isRecursive: Boolean): Outcome<Unit, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun list(dir: KMPFile, isRecursive: Boolean): Outcome<List<KMPFile>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun readMetadata(file: KMPFile): Outcome<FileMetadata, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(file: KMPFile): Boolean {
        TODO("Not yet implemented")
    }
}

actual fun KMPFile.source(): Outcome<Source, Exception> {
    TODO()
}

actual fun KMPFile.sink(mode: KMPFileWriteMode): Outcome<Sink, Exception> {
    TODO()
}
