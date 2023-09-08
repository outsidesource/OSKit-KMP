package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement

@Serializable
@SerialName("type")
sealed class DevToolClientEvent {
    @Serializable
    @SerialName("json")
    data class Json(
        val msgId: Int,
        val jsonId: String,
        val time: Long = Clock.System.now().toEpochMilliseconds(),
        val message: String,
        val json: JsonElement,
    ) : DevToolClientEvent()

    @Serializable
    @SerialName("log")
    data class Log(
        val msgId: Int,
        val time: Long = Clock.System.now().toEpochMilliseconds(),
        val message: String,
    ) : DevToolClientEvent()

    @Serializable
    @SerialName("status")
    sealed class Status : DevToolClientEvent() {
        data class Error(val error: DevToolClientError) : Status()
        object Connected : Status()
    }
}

suspend fun DevToolClientEvent.Companion.deserialize(event: String): DevToolClientEvent =
    withContext(devToolScope.coroutineContext) {
        devToolJson.decodeFromString(event)
    }

@Serializable
sealed class DevToolClientError(override val message: String = "") : Throwable(message = message) {
    data object ServerClosed : DevToolClientError("Server was closed")
    data object InvalidHost : DevToolClientError("Invalid host")
    data object Unknown : DevToolClientError("Unknown")
    data object UnknownEvent : DevToolClientError("Received unknown event")
    data object Uninitialized : DevToolClientError()
}

expect class OSDevToolClient {
    fun connect(
        scheme: String = "ws://",
        host: String,
        port: Int = 7890,
        path: String = ""
    ): Flow<DevToolClientEvent>
}
