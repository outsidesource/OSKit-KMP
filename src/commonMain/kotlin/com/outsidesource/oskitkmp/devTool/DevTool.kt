package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.ExperimentalTime

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
data class DevToolEvent(
    val id: String,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val label: String,
    val json: JsonElement,
)

suspend fun DevToolEvent.Companion.deserialize(event: String): DevToolEvent =
    withContext(devToolScope.coroutineContext) {
        devToolJson.decodeFromString(event)
    }

expect class OSDevTool {
    companion object {
        internal val instance: OSDevTool
        fun init()
    }

    internal var isInitialized: Boolean
    internal suspend fun sendEvent(event: DevToolEvent)
}

@OptIn(ExperimentalTime::class)
fun OSDevTool.Companion.sendEvent(id: String, label: String, json: Any = JsonNull) {
    if (!instance.isInitialized) return

    devToolScope.launch {
        val encoded = when (json) {
            is DevToolSerializable -> json.serialize(json)
            is String -> devToolJson.parseToJsonElement(json)
            is JsonNull -> json
            else -> JsonPrimitive(json.toString())
        }
        instance.sendEvent(DevToolEvent(id = id, label = label, json = encoded))
    }
}
