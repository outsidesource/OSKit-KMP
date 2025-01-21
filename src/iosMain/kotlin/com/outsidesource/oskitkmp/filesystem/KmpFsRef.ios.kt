package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.OkIoKmpIoSink
import com.outsidesource.oskitkmp.io.OkIoKmpIoSource
import com.outsidesource.oskitkmp.io.sink
import com.outsidesource.oskitkmp.io.source
import com.outsidesource.oskitkmp.lib.Deferrer
import com.outsidesource.oskitkmp.outcome.Outcome
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSError
import platform.Foundation.NSInputStream
import platform.Foundation.NSMutableData
import platform.Foundation.NSOutputStream
import platform.Foundation.NSURL
import platform.Foundation.NSURLBookmarkCreationMinimalBookmark
import platform.Foundation.NSURLBookmarkResolutionWithoutUI
import platform.Foundation.appendBytes

actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    val deferrer = Deferrer()

    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        when (type) {
            KmpFsType.Internal -> Outcome.Ok(OkIoKmpIoSource(FileSystem.SYSTEM.source(ref.toPath())))
            KmpFsType.External -> {
                val url = toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)

                url.startAccessingSecurityScopedResource()
                deferrer.defer { url.stopAccessingSecurityScopedResource() }

                val source = NSInputStream(uRL = url).source()
                Outcome.Ok(OkIoKmpIoSource(source))
            }
        }
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    } finally {
        deferrer.run()
    }
}

actual suspend fun KmpFsRef.sink(mode: KmpFsWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    val deferrer = Deferrer()

    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        when (type) {
            KmpFsType.Internal -> Outcome.Ok(OkIoKmpIoSink(FileSystem.SYSTEM.sink(ref.toPath())))
            KmpFsType.External -> {
                val url = toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)

                url.startAccessingSecurityScopedResource()
                deferrer.defer { url.stopAccessingSecurityScopedResource() }

                val sink = NSOutputStream(uRL = url, append = mode == KmpFsWriteMode.Append).sink()
                Outcome.Ok(OkIoKmpIoSink(sink))
            }
        }
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    } finally {
        deferrer.run()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSURL.toKmpFileRef(isDirectory: Boolean): KmpFsRef {
    startAccessingSecurityScopedResource()

    val ref = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val bookmarkData = bookmarkDataWithOptions(
            options = NSURLBookmarkCreationMinimalBookmark,
            includingResourceValuesForKeys = null,
            relativeToURL = null,
            error = error.ptr,
        )
        bookmarkData?.bytes?.readBytes(bookmarkData.length.toInt())?.encodeBase64()
    }

    stopAccessingSecurityScopedResource()

    return KmpFsRef(
        ref = ref ?: "",
        name = path?.split("/")?.lastOrNull() ?: "",
        isDirectory = isDirectory,
        type = KmpFsType.External,
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun KmpFsRef.toNSURL(): NSURL? {
    val data = NSMutableData()
    ref.decodeBase64Bytes().usePinned {
        data.appendBytes(it.addressOf(0).reinterpret(), it.get().size.convert())
    }

    return memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val isStale = alloc<BooleanVar>()

        val url = NSURL(
            byResolvingBookmarkData = data,
            options = NSURLBookmarkResolutionWithoutUI,
            relativeToURL = null,
            bookmarkDataIsStale = isStale.ptr,
            error = error.ptr,
        )

        if (error.value != null) return null
        if (isStale.value) return null

        url
    }
}
