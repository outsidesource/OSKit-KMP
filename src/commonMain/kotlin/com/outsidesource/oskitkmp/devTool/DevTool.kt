package com.outsidesource.oskitkmp.devTool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

internal val devToolJson = Json { encodeDefaults = true }
internal expect val devToolScope: CoroutineScope
private val registeredSerializers: MutableMap<KClass<*>, KSerializer<*>> = mutableMapOf()

interface DevToolSerializable

fun <T : Any> devToolSerializer(clazz: KClass<T>, serializer: KSerializer<T>): DevToolSerializable {
    registeredSerializers[clazz] = serializer
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
            is DevToolSerializable ->
                (registeredSerializers[value::class] as? KSerializer<Any>)?.serialize(encoder, value)
            else -> String.serializer().serialize(encoder, value.toString())
        }
    }
}

@Serializable
data class DevToolEvent(
    val id: String,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    val label: String,
    @Serializable(with = AnySerializer::class) val json: Any,
)

expect class OSDevTool {
    companion object {
        internal val instance: OSDevTool
        fun init()
    }

    internal var isInitialized: Boolean
    internal suspend fun sendEvent(event: DevToolEvent)
}

@OptIn(ExperimentalTime::class)
fun OSDevTool.Companion.sendEvent(id: String, label: String, json: Any) {
    if (!instance.isInitialized) return

    devToolScope.launch {
        instance.sendEvent(DevToolEvent(id = id, label = label, json = json))
    }
}
