package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.io.use
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

internal suspend fun IExternalKmpFs.nonJsSaveFile(
    bytes: ByteArray,
    fileName: String,
): Outcome<Unit, KmpFsError> {
    return try {
        val file =
            pickSaveFile(fileName).unwrapOrReturn { return it } ?: return Outcome.Error(KmpFsError.RefNotPicked)
        val sink = file.sink().unwrapOrReturn { return it }
        sink.use { it.write(bytes) }
        Outcome.Ok(Unit)
    } catch (t: Throwable) {
        Outcome.Error(KmpFsError.Unknown(t))
    }
}
