package com.outsidesource.oskitkmp.devTool

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal val devToolJson = Json { encodeDefaults = true }
internal expect val devToolScope: CoroutineScope
private val registeredSerializers: MutableMap<KClass<*>, KSerializer<*>> = mutableMapOf()
private val registeredSerializersLock = SynchronizedObject()

interface DevToolSerializable

fun <T : Any> devToolSerializer(clazz: KClass<T>, serializer: KSerializer<T>): DevToolSerializable {
    synchronized(registeredSerializersLock) { registeredSerializers[clazz] = serializer }
    return object : DevToolSerializable {}
}

private object AnySerializer : KSerializer<Any> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        ContextualSerializer(Any::class, null, emptyArray()).descriptor

    override fun deserialize(decoder: Decoder): Any = -1

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> String.serializer().serialize(encoder, value)
            is DevToolSerializable -> {
                val serializer = synchronized(registeredSerializersLock) {
                    (registeredSerializers[value::class] as? KSerializer<Any>)
                }
                serializer?.serialize(encoder, value)
            }
            else -> String.serializer().serialize(encoder, value.toString())
        }
    }
}

private val msgIdCounter = atomic(0)

@Serializable
@SerialName("type")
sealed class DevToolServerEvent {
    @Serializable
    @SerialName("json")
    data class Json(
        val msgId: Int,
        val jsonId: String,
        val time: Long = Clock.System.now().toEpochMilliseconds(),
        val message: String,
        @Serializable(AnySerializer::class) val json: Any,
    ) : DevToolServerEvent()

    @Serializable
    @SerialName("log")
    data class Log(
        val msgId: Int,
        val time: Long = Clock.System.now().toEpochMilliseconds(),
        val message: String,
    ) : DevToolServerEvent()
}

expect class OSDevTool {
    companion object {
        internal val instance: OSDevTool
        fun init()
    }

    internal var isInitialized: Boolean
    internal suspend fun sendEvent(event: DevToolServerEvent)
}

fun OSDevTool.Companion.sendEvent(id: String, message: String, json: Any) {
    if (!instance.isInitialized) return

    devToolScope.launch {
        instance.sendEvent(
            DevToolServerEvent.Json(msgId = msgIdCounter.getAndIncrement(), jsonId = id, message = message, json = json)
        )
    }
}

fun OSDevTool.Companion.sendEvent(message: String) {
    if (!instance.isInitialized) return

    devToolScope.launch {
        instance.sendEvent(DevToolServerEvent.Log(msgId = msgIdCounter.getAndIncrement(), message = message))
    }
}
