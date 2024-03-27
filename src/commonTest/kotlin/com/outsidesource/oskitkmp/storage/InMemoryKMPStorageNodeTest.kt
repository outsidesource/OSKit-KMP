package com.outsidesource.oskitkmp.storage

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class InMemoryKMPStorageNodeTest {
    @Test
    fun testGeneralUse() {
        val node = InMemoryKMPStorageNode()

        assertTrue("getIntNull") { node.getInt("int") == null }
        assertTrue("getLongNull") { node.getLong("long") == null }
        assertTrue("getBytesNull") { node.getBytes("bytes") == null }
        assertTrue("getFloatNull") { node.getFloat("float") == null }
        assertTrue("getDoubleNull") { node.getDouble("double") == null }
        assertTrue("getStringNull") { node.getString("string") == null }
        assertTrue("getSerializableNull") { node.getSerializable("serializable", TestSerializable.serializer()) == null }

        node.putInt("int", 0)
        node.putLong("long", 1L)
        node.putBytes("bytes", byteArrayOf(0x00, 0x01, 0x02))
        node.putFloat("float", 2.123f)
        node.putDouble("double", 3.123)
        node.putString("string", "test string")
        node.putSerializable("serializable", TestSerializable(1, "two"), TestSerializable.serializer())

        assertTrue("keyCount") { node.keyCount() == 7L }
        assertTrue("getInt") { node.getInt("int") == 0 }
        assertTrue("getLong") { node.getLong("long") == 1L }
        assertTrue("getBytes") { node.getBytes("bytes").contentEquals(byteArrayOf(0x00, 0x01, 0x02)) }
        assertTrue("getFloat") { node.getFloat("float") == 2.123f }
        assertTrue("getDouble") { node.getDouble("double") == 3.123 }
        assertTrue("getString") { node.getString("string") == "test string" }
        assertTrue("getSerializable") { node.getSerializable("serializable", TestSerializable.serializer()) == TestSerializable(1, "two") }

        node.clear()
        assertTrue("clear") { node.keyCount() == 0L }
    }

    @Test
    fun testObserve() = runBlocking {
        val node = InMemoryKMPStorageNode()
        var lastValue: Any? = null
        val observer = async { node.observeInt("int").take(2).collect { lastValue = it } }
        delay(16)
        node.putInt("int", 1)
        node.putInt("int2", 0)
        node.putInt("int2", 0)
        node.putInt("int", 2)
        node.remove("int")
        observer.await()
        assertTrue("lastValue == $lastValue") { lastValue == 2 }
    }

    @Test
    fun testTransaction() {
        val node = InMemoryKMPStorageNode()

        node.putInt("int", 0)
        node.putLong("long", 1L)
        node.putBytes("bytes", byteArrayOf(0x00, 0x01, 0x02))
        node.putFloat("float", 2.123f)
        node.putDouble("double", 3.123)
        node.putString("string", "test string")
        node.putSerializable("serializable", TestSerializable(1, "two"), TestSerializable.serializer())

        node.transaction {
            node.remove("int")
            node.putLong("long", 2L)
        }

        assertTrue("transaction") {
            node.keyCount() == 6L &&
                    node.getLong("long") == 2L &&
                    node.getFloat("float") == 2.123f &&
                    !node.contains("testString")  &&
                    !node.contains("testString")
        }

        node.transaction { rollback ->
            node.remove("long")
            node.putFloat("float", 10f)
            node.putString("testString", "this should not exist")
            node.putString("testString2", "this should not exist")

            assertTrue("transactionValuesUpdated") { node.keyCount() == 7L && !node.contains("long") && node.getFloat("float") == 10f  }
            rollback()
        }

        assertTrue("transaction") {
            node.keyCount() == 6L &&
                    node.getLong("long") == 2L &&
                    node.getFloat("float") == 2.123f &&
                    !node.contains("testString")  &&
                    !node.contains("testString")
        }
    }
}