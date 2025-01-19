package com.outsidesource.oskitkmp.filesystem

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

actual suspend fun onKmpFileRefPersisted(ref: KmpFsRef) {}
actual suspend fun internalClearPersistedDataCache(ref: KmpFsRef?) {}

internal suspend fun IKmpFs.nonJsSaveFile(
    bytes: ByteArray,
    fileName: String,
): Outcome<Unit, Throwable> {
    return try {
        val file = pickSaveFile(fileName).unwrapOrReturn { return it } ?: return Outcome.Error(FileNotPicked())
        val sink = file.sink().unwrapOrReturn { return it }
        sink.use { it.write(bytes) }
        Outcome.Ok(Unit)
    } catch (t: Throwable) {
        Outcome.Error(t)
    }
}
