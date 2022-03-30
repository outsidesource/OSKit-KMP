package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement

@Serializable
data class DevToolClientEvent(
    val id: String,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val label: String,
    val json: JsonElement,
)

suspend fun DevToolClientEvent.Companion.deserialize(event: String): DevToolClientEvent =
    withContext(devToolScope.coroutineContext) {
        devToolJson.decodeFromString(event)
    }

sealed class OSDevToolClientError(override val message: String = "") : Throwable(message = message) {
    object ServerClosed : OSDevToolClientError("Server was closed")
    object InvalidHost : OSDevToolClientError("Invalid host")
    object Unknown : OSDevToolClientError("Unknown")
    object UnknownEvent : OSDevToolClientError("Received unknown event")
    object Uninitialized : OSDevToolClientError()
}

expect class OSDevToolClient {
    fun connect(host: String, port: Int): Flow<DevToolClientEvent>
}
