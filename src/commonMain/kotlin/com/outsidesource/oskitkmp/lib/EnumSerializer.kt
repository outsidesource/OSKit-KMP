package com.outsidesource.oskitkmp.lib

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.enums.enumEntries

/**
 * A `kotlinx.serialization` serializer that allow serialization to/from a specific value type while also allowing
 * for an optional default value. The optional value only works for values of the same type. KotlinX Serialization
 * throws an exception for anything of an unexpected type.
 *
 * NOTE: @SerialName does not work with this serializer. Use your own custom value to change the serialized value.
 */
inline fun <reified T : Enum<T>> EnumSerializer(
    crossinline value: (T) -> Any,
    default: T? = null,
): KSerializer<T> = object : KSerializer<T> {

    private val entries = enumEntries<T>()
    private val valueMap = entries.associateBy { value(it) }

    private val primitiveKind: PrimitiveKind by lazy {
        when (val valueType = value(entries.first())) {
            is Boolean -> PrimitiveKind.BOOLEAN
            is Byte -> PrimitiveKind.BYTE
            is Char -> PrimitiveKind.CHAR
            is Double -> PrimitiveKind.DOUBLE
            is Float -> PrimitiveKind.FLOAT
            is Int -> PrimitiveKind.INT
            is Long -> PrimitiveKind.LONG
            is Short -> PrimitiveKind.SHORT
            is String -> PrimitiveKind.STRING
            else -> error("Unsupported enum type: ${valueType::class}")
        }
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("${entries.first()::class.simpleName}", primitiveKind)

    private fun Encoder.encodeAny(value: Any) = when (primitiveKind) {
        PrimitiveKind.BOOLEAN -> encodeBoolean(value as Boolean)
        PrimitiveKind.BYTE -> encodeByte(value as Byte)
        PrimitiveKind.CHAR -> encodeChar(value as Char)
        PrimitiveKind.DOUBLE -> encodeDouble(value as Double)
        PrimitiveKind.FLOAT -> encodeFloat(value as Float)
        PrimitiveKind.INT -> encodeInt(value as Int)
        PrimitiveKind.LONG -> encodeLong(value as Long)
        PrimitiveKind.SHORT -> encodeShort(value as Short)
        PrimitiveKind.STRING -> encodeString(value as String)
    }

    private fun Decoder.decodeAny(): Any = when (primitiveKind) {
        PrimitiveKind.BOOLEAN -> decodeBoolean()
        PrimitiveKind.BYTE -> decodeByte()
        PrimitiveKind.CHAR -> decodeChar()
        PrimitiveKind.DOUBLE -> decodeDouble()
        PrimitiveKind.FLOAT -> decodeFloat()
        PrimitiveKind.INT -> decodeInt()
        PrimitiveKind.LONG -> decodeLong()
        PrimitiveKind.SHORT -> decodeShort()
        PrimitiveKind.STRING -> decodeString()
    }

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeAny(value(value))

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeAny()
        return valueMap[raw] ?: default ?: throw SerializationException(
            "Unknown enum value '$raw' for ${descriptor.serialName}",
        )
    }
}
