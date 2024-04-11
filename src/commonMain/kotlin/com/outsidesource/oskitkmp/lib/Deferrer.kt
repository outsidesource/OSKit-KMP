package com.outsidesource.oskitkmp.lib

class Deferrer {
    private var defer: (() -> Unit)? = null

    fun defer(block: () -> Unit) {
        defer = block
    }

    fun run() = defer?.invoke()
}

class SuspendDeferrer {
    private var defer: (suspend () -> Unit)? = null

    fun defer(block: suspend () -> Unit) {
        defer = block
    }

    suspend fun run() = defer?.invoke()
}

inline fun <T> defer(block: (deferrer: Deferrer) -> T): T {
    val deferrer = Deferrer()

    return try {
        block(deferrer)
    } finally {
        deferrer.run()
    }
}

suspend inline fun <T> suspendDefer(block: (deferrer: SuspendDeferrer) -> T): T {
    val deferrer = SuspendDeferrer()

    return try {
        block(deferrer)
    } finally {
        deferrer.run()
    }
}
