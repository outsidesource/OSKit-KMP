package com.outsidesource.oskitkmp.file

import com.outsidesource.oskitkmp.outcome.Outcome
import okio.Buffer
import okio.Source
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun createSourceFromJsFile(file: File): Outcome<Source, Exception> = suspendCoroutine { continuation ->
    val reader = FileReader()
    reader.onload = {
        val arrayBuffer = reader.result as ArrayBuffer
        val uint8Array = Uint8Array(arrayBuffer)
        val byteArray = ByteArray(uint8Array.length) { uint8Array[it] }
        val buffer = Buffer().write(byteArray)
        continuation.resume(Outcome.Ok(buffer))
    }
    reader.onerror = { continuation.resume(Outcome.Error(FileOpenError())) }
    reader.readAsArrayBuffer(file)
}
