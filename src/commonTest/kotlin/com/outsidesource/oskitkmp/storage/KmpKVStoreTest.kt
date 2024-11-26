package com.outsidesource.oskitkmp.storage

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.uuid.ExperimentalUuidApi

class InMemoryKmpKVStoreTest : IKmpKVStoreTest {
    override val kvStore: IKmpKVStore = InMemoryKmpKVStore()
}

interface IKmpKVStoreTest {
    val kvStore: IKmpKVStore

    private suspend fun openNode() = kvStore.openNode("Test").unwrapOrReturn { fail("Could not open node") }

    @OptIn(ExperimentalUuidApi::class, ExperimentalStdlibApi::class)
    @Test
    fun keyGeneration() = runBlockingTest {
        val node = openNode()

        val keys = mutableSetOf<String>()
        for (i in 0..1_000_000) {
            val newKey = node.createUniqueKey()
            if (keys.contains(newKey)) fail("Key was not unique")
            keys.add(newKey)
        }
    }

    @Test
    fun createNode() = runBlockingTest {
        val outcome = kvStore.openNode("Test")
        if (outcome is Outcome.Error) fail("Could not open node: ${outcome.error}")
    }

    @Test
    fun putGetBytes() = runBlockingTest {
        val node = openNode()
        node.putBytes("test", byteArrayOf(0x00, 0x01, 0x02)).unwrapOrReturn { fail() }
        assertTrue { node.getBytes("test").contentEquals(byteArrayOf(0x00, 0x01, 0x02)) }
    }

    @Test
    fun putGetString() = runBlockingTest {
        val node = openNode()
        node.putString("test", "Hello").unwrapOrReturn { fail() }
        assertTrue { node.getString("test") == "Hello" }
    }

    @Test
    fun putGetInt() = runBlockingTest {
        val node = openNode()
        node.putInt("test", 1).unwrapOrReturn { fail() }
        assertTrue { node.getInt("test") == 1 }
    }

    @Test
    fun putGetLong() = runBlockingTest {
        val node = openNode()
        node.putLong("test", 1L).unwrapOrReturn { fail() }
        assertTrue { node.getLong("test") == 1L }
    }

    @Test
    fun putGetFloat() = runBlockingTest {
        val node = openNode()
        node.putFloat("test", 1.1234567f).unwrapOrReturn { fail() }
        assertTrue { node.getFloat("test") == 1.1234567f }
    }

    @Test
    fun putGetDouble() = runBlockingTest {
        val node = openNode()
        node.putDouble("test", 1.1234567890123457).unwrapOrReturn { fail() }
        assertTrue { node.getDouble("test") == 1.1234567890123457 }
    }

    @Test
    fun putGetBoolean() = runBlockingTest {
        val node = openNode()
        node.putBoolean("test", true).unwrapOrReturn { fail() }
        assertTrue { node.getBoolean("test") == true }
    }

    @Test
    fun putGetSerializable() = runBlockingTest {
        val node = openNode()
        node.putSerializable("test", TestSerializable(1, "one"), TestSerializable.serializer()).unwrapOrReturn { fail() }
        assertTrue { node.getSerializable("test", TestSerializable.serializer()) == TestSerializable(1, "one") }
    }

    @Test
    fun observeBytes() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeBytes("test").first() } }
        delay(16)
        node.putBytes("test", byteArrayOf(0x00, 0x01, 0x02))
        assertTrue { value.await().contentEquals(byteArrayOf(0x00, 0x01, 0x02)) }
    }

    @Test
    fun observeString() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeString("test").first() } }
        delay(16)
        node.putString("test", "Hello")
        assertTrue { value.await() == "Hello" }
    }

    @Test
    fun observeInt() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeInt("test").first() } }
        delay(16)
        node.putInt("test", 1)
        assertTrue { value.await() == 1 }
    }

    @Test
    fun observeLong() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeLong("test").first() } }
        delay(16)
        node.putLong("test", 1L)
        assertTrue { value.await() == 1L }
    }

    @Test
    fun observeFloat() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeFloat("test").first() } }
        delay(16)
        node.putFloat("test", 1.1234567f)
        assertTrue { value.await() == 1.1234567f }
    }

    @Test
    fun observeDouble() = runBlockingTest {
        val node = openNode()
        val value = async { node.observeDouble("test").first() }
        delay(16)
        node.putDouble("test", 1.1234567890123457)
        assertTrue { value.await() == 1.1234567890123457 }
    }

    @Test
    fun observeBoolean() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeBoolean("test").first() } }
        delay(16)
        node.putBoolean("test", true)
        assertTrue { value.await() == true }
    }

    @Test
    fun observeSerializable() = runBlockingTest {
        val node = openNode()
        val value = async { withTimeout(100) { node.observeSerializable("test", TestSerializable.serializer()).first() } }
        delay(16)
        node.putSerializable("test", TestSerializable(1, "one"), TestSerializable.serializer())
        assertTrue { value.await() == TestSerializable(1, "one") }
    }

    @Test
    fun multipleObserve() = runBlockingTest {
        val node = openNode()
        node.clear()
        var lastValue: Any? = null
        val observer = async { node.observeInt("int").take(3).collect { lastValue = it } }
        delay(16)
        node.putInt("int", 1)
        node.putInt("int2", 0)
        node.putInt("int2", 0)
        node.putInt("int", 2)
        node.putInt("int", 4)
        observer.await()
        assertTrue("lastValue == $lastValue") { lastValue == 4 }
    }

    @Test
    fun observeRaceConditionTest() = runBlockingTest {
        val node = openNode()
        node.clear()
        var notifications = mutableListOf<Int>()
        val testNotifications = List<Int>(100) { it }
        val observer = async { node.observeInt("int").take(100).filterNotNull().collect { notifications.add(it) } }
        delay(16)
        for (i in 0 until 100) {
            node.putInt("int", i)
        }
        observer.await()
        assertTrue("received notifications out of order - $notifications") { notifications == testNotifications }
    }

    @Test
    fun testRemove() = runBlockingTest {
        val node = openNode()
        node.clear()
        node.putInt("testInt", 1)
        assertTrue("Write didn't work") { node.contains("testInt") }
        node.remove("testInt").unwrapOrReturn { fail("result was an error") }
        assertTrue("Remove didn't work") { !node.contains("testInt") }
    }

    @Test
    fun testObserveRemoval() = runBlockingTest {
        val node = openNode()
        node.clear()
        var lastValue: Any? = null
        val observer = async { node.observeInt("int").take(4).collect { lastValue = it } }
        delay(16)
        node.putInt("int", 1)
        node.remove("int")
        node.putInt("int", 1)
        node.clear()
        observer.await()
        assertTrue("lastValue == $lastValue") { lastValue == null }
    }

    @Test
    fun testObserveDistinctRemoval() = runBlockingTest {
        val node = openNode()
        node.clear()
        var lastValue: Any? = null
        val observer = async { node.observeInt("int").take(2).collect { lastValue = it } }
        delay(16)
        node.putInt("int", 1)
        node.putInt("int", 1)
        node.putInt("int", 1)
        delay(16)
        node.putInt("int", 1)
        node.putInt("int", 2)
        observer.await()
        assertTrue("lastValue == $lastValue") { lastValue == 2 }
    }

    @Test
    fun multipleNodeObserve() = runBlockingTest {
        val node1 = openNode()
        val node2 = openNode()
        node1.clear()
        val count = atomic(0)
        val observer1 = async { node1.observeInt("int").take(3).collect { count.incrementAndGet() } }
        val observer2 = async { node2.observeInt("int").take(3).collect { count.incrementAndGet() } }
        delay(16)
        node1.putInt("int", 1)
        node1.putInt("int2", 0)
        node1.putInt("int2", 0)
        node1.putInt("int", 2)
        node1.remove("int")
        observer1.await()
        observer2.await()
        assertTrue("notification count == ${count.value}") { count.value == 6 }
    }

    @Test
    fun clear() = runBlockingTest {
        val node = openNode()
        node.clear()
        node.putInt("test", 1).unwrapOrReturn { fail("Could not insert") }
        node.putInt("test2", 1).unwrapOrReturn { fail("Could not insert") }
        node.putInt("test3", 1).unwrapOrReturn { fail("Could not insert") }
        assertTrue(message = "Insert didn't work") { node.keyCount() == 3L }
        node.clear().unwrapOrReturn { fail("result was error") }
        assertTrue(message = "Clear didn't work") { node.keyCount() == 0L }
    }

    @Test
    fun keyCount() = runBlockingTest {
        val node = openNode()
        node.clear()
        assertTrue { node.keyCount() == 0L }
        node.putInt("test", 1).unwrapOrReturn { fail("Could not insert") }
        assertTrue { node.keyCount() == 1L }
    }

    @Test
    fun keys() = runBlockingTest {
        val node = openNode()
        node.clear()
        node.putString("test1", "Hello 1")
        node.putString("test2", "Hello 2")
        assertTrue { node.keys() == setOf("test1", "test2") }
    }

    @Test
    fun exists() = runBlockingTest {
        val node = openNode()
        node.clear()
        assertTrue { !node.contains("test") }
        node.putInt("test", 1)
        assertTrue { node.contains("test") }
    }

    @Test
    fun testTransaction() = runBlockingTest {
        val node = openNode()
        node.clear()

        node.putInt("int", 1)
        node.putLong("long", 1L)
        node.putFloat("float", 1.123f)
        node.putDouble("double", 1.123)
        node.putString("string", "hello")
        node.putBoolean("boolean", true)
        node.putBytes("bytes", byteArrayOf(0x00, 0x01, 0x02))
        node.putSerializable("serializable", TestSerializable(one = 1, two = "one"), TestSerializable.serializer())

        // Test reverting to previous values
        node.transaction { rollback ->
            node.putInt("int", 2)
            node.putLong("long", 2L)
            node.putFloat("float", 2.123f)
            node.putDouble("double", 2.123)
            node.putString("string", "hello2")
            node.putBoolean("boolean", false)
            node.putBytes("bytes", byteArrayOf(0x01, 0x02, 0x03))
            node.putSerializable("serializable", TestSerializable(one = 2, two = "two"), TestSerializable.serializer())

            assertTrue("values not updated in transaction") {
                println(node.getInt("int"))
                println(node.getLong("long"))
                println(node.getFloat("float"))
                println(node.getDouble("double"))
                println(node.getString("string"))
                println(node.getBoolean("boolean"))
                println(node.getBytes("bytes"))
                println(node.getSerializable("serializable", TestSerializable.serializer()))


                node.getInt("int") == 2 &&
                    node.getLong("long") == 2L &&
                    node.getFloat("float") == 2.123f &&
                    node.getDouble("double") == 2.123 &&
                    node.getString("string") == "hello2" &&
                    node.getBoolean("boolean") == false &&
                    node.getBytes("bytes").contentEquals(byteArrayOf(0x01, 0x02, 0x03)) &&
                    node.getSerializable("serializable", TestSerializable.serializer()) == TestSerializable(one = 2, two = "two")
            }
            rollback()
        }

        assertTrue("values were not changed when rolled back") {
            node.getInt("int") == 1 &&
                    node.getLong("long") == 1L &&
                    node.getFloat("float") == 1.123f &&
                    node.getDouble("double") == 1.123 &&
                    node.getString("string") == "hello" &&
                    node.getBoolean("boolean") == true &&
                    node.getBytes("bytes").contentEquals(byteArrayOf(0x00, 0x01, 0x02)) &&
                    node.getSerializable("serializable", TestSerializable.serializer()) == TestSerializable(one = 1, two = "one")
        }

        // Test reverting to removed value
        node.clear()
        node.transaction { rollback ->
            node.putInt("int", 1)
            node.putLong("long", 1L)
            node.putFloat("float", 1.123f)
            node.putDouble("double", 1.123)
            node.putString("string", "hello")
            node.putBoolean("boolean", true)
            node.putBytes("bytes", byteArrayOf(0x00, 0x01, 0x02))
            node.putSerializable("serializable", TestSerializable(one = 1, two = "one"), TestSerializable.serializer())
            rollback()
        }

        assertTrue("values were not deleted when rolled back") {
            node.getInt("int") == null &&
                node.getLong("long") == null &&
                node.getFloat("float") == null &&
                node.getDouble("double") == null &&
                node.getString("string") == null &&
                node.getBoolean("boolean") == null &&
                node.getBytes("bytes") == null &&
                node.getSerializable("serializable", TestSerializable.serializer()) == null
        }
    }

    @Test
    fun testWrongTypes() = runBlockingTest {
        val node = InMemoryKmpKVStoreNode("test")
        node.putString("test", "Test")
        assertTrue { node.getLong("test") == null }
        node.putLong("test", 1L)
        assertTrue { node.getLong("test") == 1L }
    }
}

@Serializable
internal data class TestSerializable(val one: Int, val two: String)