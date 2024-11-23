package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowExtTest {
    @Test
    fun throttleTest() = blockingTest {
        val emissions = mutableListOf<Int>()
        val flow = flow {
            for (i in 0..10) {
                emit(i)
                delay(100)
            }
        }

        flow.throttle(500).collect { emissions.add(it) }
        assertEquals(emissions, listOf(0, 5, 10))
    }

    @Test
    fun flowInTest() = blockingTest {
        val flow = flow {
            for (i in 0..10) {
                emit(i)
                delay(1000)
            }
        }

        val scope = CoroutineScope(KMPDispatchers.IO + SupervisorJob())

        coroutineScope {
            launch {
                flow.flowIn(scope).collect {}
            }

            delay(200)
            scope.cancel()
        }

        // If this test doest not block indefinitely, the test passes
    }
}

private fun blockingTest(block: suspend CoroutineScope.() -> Unit) = runTest {
    withContext(Dispatchers.Default) {
        block()
    }
}