package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class KMPStorageTest {
    private val storage = createKMPStorage()

    private fun openNode() = storage.openNode("Test").unwrapOrReturn { fail("Could not open node") }
    
    @Test
    fun createNode() = blockingTest {
        val outcome = storage.openNode("Test")
        if (outcome is Outcome.Error) fail("Could not open node: ${outcome.error}")
    }

    @Test
    fun putGetBytes() = blockingTest {
        val node = openNode()
        node.putBytes("test", byteArrayOf(0x00, 0x01, 0x02)).unwrapOrReturn { fail() }
        assertTrue { node.getBytes("test").contentEquals(byteArrayOf(0x00, 0x01, 0x02)) }
    }

    @Test
    fun putGetString() = blockingTest {
        val node = openNode()
        node.putString("test", "Hello").unwrapOrReturn { fail() }
        assertTrue { node.getString("test") == "Hello" }
    }

    @Test
    fun putGetInt() = blockingTest {
        val node = openNode()
        node.putInt("test", 1).unwrapOrReturn { fail() }
        assertTrue { node.getInt("test") == 1 }
    }

    @Test
    fun putGetLong() = blockingTest {
        val node = openNode()
        node.putLong("test", 1L).unwrapOrReturn { fail() }
        assertTrue { node.getLong("test") == 1L }
    }

    @Test
    fun putGetFloat() = blockingTest {
        val node = openNode()
        node.putFloat("test", 1.1234567f).unwrapOrReturn { fail() }
        assertTrue { node.getFloat("test") == 1.1234567f }
    }

    @Test
    fun putGetDouble() = blockingTest {
        val node = openNode()
        node.putDouble("test", 1.1234567890123457).unwrapOrReturn { fail() }
        assertTrue { node.getDouble("test") == 1.1234567890123457 }
    }

    @Test
    fun putGetBoolean() = blockingTest {
        val node = openNode()
        node.putBoolean("test", true).unwrapOrReturn { fail() }
        assertTrue { node.getBoolean("test") == true }
    }

    @Test
    fun putGetSerializable() = blockingTest {
        val node = openNode()
        node.putSerializable("test", TestSerializable(1, "one"), TestSerializable.serializer()).unwrapOrReturn { fail() }
        assertTrue { node.getSerializable("test", TestSerializable.serializer()) == TestSerializable(1, "one") }
    }

    @Test
    fun observeBytes() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeBytes("test").first() } }
        delay(16)
        node.putBytes("test", byteArrayOf(0x00, 0x01, 0x02))
        assertTrue { value.await().contentEquals(byteArrayOf(0x00, 0x01, 0x02)) }
    }

    @Test
    fun observeString() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeString("test").first() } }
        delay(16)
        node.putString("test", "Hello")
        assertTrue { value.await() == "Hello" }
    }

    @Test
    fun observeInt() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeInt("test").first() } }
        delay(16)
        node.putInt("test", 1)
        assertTrue { value.await() == 1 }
    }

    @Test
    fun observeLong() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeLong("test").first() } }
        delay(16)
        node.putLong("test", 1L)
        assertTrue { value.await() == 1L }
    }

    @Test
    fun observeFloat() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeFloat("test").first() } }
        delay(16)
        node.putFloat("test", 1.1234567f)
        assertTrue { value.await() == 1.1234567f }
    }

    @Test
    fun observeDouble() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeDouble("test").first() } }
        delay(16)
        node.putDouble("test", 1.1234567890123457)
        assertTrue { value.await() == 1.1234567890123457 }
    }

    @Test
    fun observeBoolean() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeBoolean("test").first() } }
        delay(16)
        node.putBoolean("test", true)
        assertTrue { value.await() }
    }

    @Test
    fun observeSerializable() = blockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeSerializable("test", TestSerializable.serializer()).first() } }
        delay(16)
        node.putSerializable("test", TestSerializable(1, "one"), TestSerializable.serializer())
        assertTrue { value.await() == TestSerializable(1, "one") }
    }

    @Test
    fun multipleObserve() = blockingTest {
        val node = openNode()
        node.clear()
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
    fun clear() = blockingTest {
        val node = openNode()
        node.clear()
        node.putInt("test", 1).unwrapOrReturn { fail("Could not insert") }
        node.putInt("test2", 1).unwrapOrReturn { fail("Could not insert") }
        node.putInt("test3", 1).unwrapOrReturn { fail("Could not insert") }
        assertTrue(message = "Insert didn't work") { node.keyCount() == 3L }
        node.clear()
        assertTrue(message = "Clear didn't work") { node.keyCount() == 0L }
    }

    @Test
    fun keyCount() = blockingTest {
        val node = openNode()
        node.clear()
        assertTrue { node.keyCount() == 0L }
        node.putInt("test", 1).unwrapOrReturn { fail("Could not insert") }
        assertTrue { node.keyCount() == 1L }
    }

    @Test
    fun keys() = blockingTest {
        val node = openNode()
        node.clear()
        node.putString("test1", "Hello 1")
        node.putString("test2", "Hello 2")
        assertTrue { node.getKeys() == setOf("test1", "test2") }
    }

    @Test
    fun exists() = blockingTest {
        val node = openNode()
        node.clear()
        assertTrue { !node.contains("test") }
        node.putInt("test", 1)
        assertTrue { node.contains("test") }
    }

    @Test
    fun testTransaction() {
        val node = openNode()
        node.clear()

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

        assertTrue("transaction 1") {
            node.keyCount() == 6L &&
                    node.getLong("long") == 2L &&
                    node.getFloat("float") == 2.123f &&
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

        assertTrue("transaction 2") {
            node.keyCount() == 6L &&
                    node.getLong("long") == 2L &&
                    node.getFloat("float") == 2.123f &&
                    !node.contains("testString")
        }
    }

    @Test
    fun testWrongTypes() {
        val node = InMemoryKMPStorageNode()
        node.putString("test", "Test")
        assertTrue { node.getLong("test") == null }
        node.putLong("test", 1L)
        assertTrue { node.getLong("test") == 1L }
    }
}

private fun blockingTest(block: suspend CoroutineScope.() -> Unit) = runTest {
    withContext(Dispatchers.Default) {
        block()
    }
}

@Serializable
internal data class TestSerializable(val one: Int, val two: String)

expect fun createKMPStorage(): IKMPStorage