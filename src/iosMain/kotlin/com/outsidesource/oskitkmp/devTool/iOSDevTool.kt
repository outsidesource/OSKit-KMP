package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal actual val devToolScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

actual class OSDevTool {
    actual companion object {
        internal actual val instance by lazy { OSDevTool() }
        actual fun init() {}
    }

    actual var isInitialized = false
    internal actual suspend fun sendEvent(event: DevToolEvent) {}
}
