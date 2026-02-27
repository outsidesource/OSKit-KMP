package com.outsidesource.oskitkmp.concurrency

import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RcCoroutineTest {
    @Test
    fun testCore() = runBlockingTest {
        var starts = 0
        var stops = 0

        val rc = RcCoroutine()

        val block: suspend () -> Unit = suspend {
            starts++
            coroutineScope {
                launch { while (currentCoroutineContext().isActive) { delay(100) } }
                    .invokeOnCompletion { stops++ }
            }
        }

        rc.start("test", block)
        rc.start("test", block)
        rc.start("test", block)
        rc.start("test", block)
        rc.start("test", block)
        delay(16)

        assertEquals(1, starts, "Started too many times")

        rc.done("test")
        rc.done("test")
        rc.done("test")
        rc.done("test")
        delay(16)

        assertEquals(0, stops, "Stopped before done")

        rc.done("test")
        delay(16)

        assertEquals(1, stops, "Didn't stop")

        rc.start("test", block)
        delay(16)
        assertEquals(2, starts, "Started too many times")

        rc.cancel("test")
        delay(16)
        assertEquals(2, stops, "Cancel didn't stop")
    }
}