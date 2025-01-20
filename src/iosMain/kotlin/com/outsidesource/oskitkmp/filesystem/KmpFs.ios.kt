package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.IKmpIoSink
import com.outsidesource.oskitkmp.io.IKmpIoSource
import com.outsidesource.oskitkmp.io.OkIoKmpIoSink
import com.outsidesource.oskitkmp.io.OkIoKmpIoSource
import com.outsidesource.oskitkmp.io.sink
import com.outsidesource.oskitkmp.io.source
import com.outsidesource.oskitkmp.lib.Deferrer
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrNull
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import io.ktor.util.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import platform.Foundation.*
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

actual data class KmpFsContext(val rootController: UIViewController)

actual suspend fun KmpFsRef.source(): Outcome<IKmpIoSource, KmpFsError> {
    val deferrer = Deferrer()

    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        val url = toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)

        url.startAccessingSecurityScopedResource()
        deferrer.defer { url.stopAccessingSecurityScopedResource() }

        val source = NSInputStream(uRL = url).source()
        Outcome.Ok(OkIoKmpIoSource(source))
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    } finally {
        deferrer.run()
    }
}

actual suspend fun KmpFsRef.sink(mode: KmpFileWriteMode): Outcome<IKmpIoSink, KmpFsError> {
    val deferrer = Deferrer()

    return try {
        if (isDirectory) return Outcome.Error(KmpFsError.RefIsDirectoryReadWriteError)
        val url = toNSURL() ?: return Outcome.Error(KmpFsError.InvalidRef)

        url.startAccessingSecurityScopedResource()
        deferrer.defer { url.stopAccessingSecurityScopedResource() }

        val sink = NSOutputStream(uRL = url, append = mode == KmpFileWriteMode.Append).sink()
        Outcome.Ok(OkIoKmpIoSink(sink))
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
