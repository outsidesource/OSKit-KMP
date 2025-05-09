package com.outsidesource.oskitkmp.lib

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class EnumSerializerTest {
    val json = Json { encodeDefaults = true }

    @Test
    fun test() {
        assertEquals("1", json.encodeToString(TestEnum.One), "Failed to serialize")
        assertEquals(TestEnum.One, json.decodeFromString<TestEnum>("1"), "Failed to deserialize")
        assertEquals(TestEnum.One, json.decodeFromString<TestEnum>("12"), "Failed to support wrong type")
        assertFails("Didn't fail with wrong type") { json.decodeFromString<TestEnum>("asdf") }
        assertFails("Didn't fail with unsupported type") { json.encodeToString(TestEnumUnsupportedType.One) }

        assertEquals("""{"test":2}""", json.encodeToString(Foo()), "Failed to serialize data class")
        assertEquals(TestEnum.Two, json.decodeFromString<Foo>("""{"test": 2}""").test, "Failed to deserialize data class")
        assertEquals(TestEnum.One, json.decodeFromString<Foo>("""{"test": 45}""").test, "Failed to deserialize data class with unknown value")
    }
}

@Serializable(with = TestEnum.Serializer::class)
private enum class TestEnum(val value: Int) {
    One(1),
    Two(2),
    Three(3),
    ;

    object Serializer: KSerializer<TestEnum> by EnumSerializer(TestEnum::value, default = One)
}

@Serializable(with = TestEnumUnsupportedType.Serializer::class)
private enum class TestEnumUnsupportedType(val value: ByteArray) {
    One(byteArrayOf()),
    ;

    object Serializer: KSerializer<TestEnumUnsupportedType> by EnumSerializer(TestEnumUnsupportedType::value, default = One)
}

@Serializable
private data class Foo(
    val test: TestEnum = TestEnum.Two,
)
