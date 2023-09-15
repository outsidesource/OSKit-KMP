package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Sink
import okio.Source
import platform.UIKit.UIViewController

actual data class KMPFileHandlerContext(
    val viewController: UIViewController
)

class IOSKMPFileHandler : IKMPFileHandler {
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

    override suspend fun readMetadata(file: KMPFileURI): Outcome<KMPFileMetadata, Exception> {
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
