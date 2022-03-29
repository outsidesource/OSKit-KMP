package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class OSDevToolClient {
    actual fun connect(
        host: String,
        port: Int,
    ): Flow<DevToolEvent> = callbackFlow {}
}
