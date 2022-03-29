package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.flow.Flow

sealed class OSDevToolClientError(override val message: String = "") : Throwable(message = message) {
    object ServerClosed : OSDevToolClientError("Server was closed")
    object InvalidHost : OSDevToolClientError("Invalid host")
    object Unknown : OSDevToolClientError("Unknown")
    object Uninitialized : OSDevToolClientError()
}

expect class OSDevToolClient {
    fun connect(host: String, port: Int): Flow<DevToolEvent>
}
