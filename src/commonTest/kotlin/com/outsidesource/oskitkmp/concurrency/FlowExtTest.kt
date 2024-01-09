package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowExtTest {
    @Test
    fun throttleTest() = runBlocking {
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
}