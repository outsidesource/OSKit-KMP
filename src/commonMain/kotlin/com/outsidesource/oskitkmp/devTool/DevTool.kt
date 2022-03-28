package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

val devToolJson = Json { encodeDefaults = true }
internal expect val devToolScope: CoroutineScope

interface DevToolSerializable {
    fun serialize(value: Any): JsonElement
}

inline fun <reified T> devToolSerializer(serializer: KSerializer<T>): DevToolSerializable =
    object : DevToolSerializable {
        override fun serialize(value: Any): JsonElement = devToolJson.encodeToJsonElement(serializer, value as T)
    }

@Serializable
sealed class DevToolEvent {
    @Serializable
    @SerialName("json")
    data class Json(
        val id: String,
        val time: Long = Clock.System.now().toEpochMilliseconds(),
        val label: String,
        val json: JsonElement,
    ) : DevToolEvent()

    @Serializable
    @SerialName("log")
    data class Log(val time: Long = Clock.System.now().toEpochMilliseconds(), val label: String) : DevToolEvent()
}

expect class OSDevTool {
    companion object {
        internal fun sendEvent(event: DevToolEvent)
    }
}

fun OSDevTool.Companion.sendJsonEvent(id: String, label: String, json: Any) {
    devToolScope.launch {
        val encoded = when (json) {
            is DevToolSerializable -> json.serialize(json)
            is String -> devToolJson.parseToJsonElement(json)
            else -> JsonPrimitive(json.toString())
        }
        sendEvent(DevToolEvent.Json(id = id, label = label, json = encoded))
    }
}

fun OSDevTool.Companion.sendLogEvent(label: String) = sendEvent(DevToolEvent.Log(label = label))

suspend fun DevToolEvent.Companion.deserialize(event: String): DevToolEvent =
    withContext(devToolScope.coroutineContext) {
        devToolJson.decodeFromString(event)
    }
