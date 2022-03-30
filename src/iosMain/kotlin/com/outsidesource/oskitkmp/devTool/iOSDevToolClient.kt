package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class OSDevToolClient {
    actual fun connect(
        scheme: String,
        host: String,
        port: Int,
        path: String,
    ): Flow<DevToolClientEvent> = callbackFlow {}
}
