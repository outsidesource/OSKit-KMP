package com.outsidesource.oskitkmp.io

import com.outsidesource.oskitkmp.outcome.Outcome

interface IKmpIoClosable {
    suspend fun close()
}

suspend inline fun <T : IKmpIoClosable, R> T.use(block: (T) -> R): Outcome<R, KmpIoError> {
    var thrown: Throwable? = null

    val result = try {
        block(this)
    } catch (t: Throwable) {
        thrown = t
        null
    } finally {
        try {
            close()
        } catch (t: Throwable) {
            if (thrown == null) thrown = t else thrown.addSuppressed(t)
        }
    }

    if (thrown != null) return Outcome.Error(KmpIoError.Unknown(thrown))

    @Suppress("UNCHECKED_CAST")
    return Outcome.Ok(result as R)
}