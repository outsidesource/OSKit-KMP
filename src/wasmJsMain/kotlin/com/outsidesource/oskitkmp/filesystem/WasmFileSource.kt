package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import okio.Buffer
import okio.Source
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.files.File

@Suppress("USELESS_IS_CHECK")
suspend fun createSourceFromJsFile(file: File): Outcome<Source, Exception> {
    if (file !is Blob) return Outcome.Error(FileOpenError())
    val arrayBuffer = file.arrayBuffer().kmpAwaitOutcome().unwrapOrReturn { return Outcome.Error(FileOpenError()) }
    val uint8Array = Uint8Array(arrayBuffer)
    val byteArray = ByteArray(uint8Array.length) { uint8Array[it] }
    val buffer = Buffer().write(byteArray)
    return Outcome.Ok(buffer)
}
