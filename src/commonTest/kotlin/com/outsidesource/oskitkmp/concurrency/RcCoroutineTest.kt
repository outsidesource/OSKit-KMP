package com.outsidesource.oskitkmp.concurrency

import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    @Test
    fun testDoneImmediatelyAfterStartStopsJob() = runBlockingTest {
        // The job may be cancelled before it ever runs, so check that it isn't left running rather than that it ran
        var isRunning = false
        val rc = RcCoroutine()

        rc.start("test") {
            isRunning = true
            try {
                awaitCancellation()
            } finally {
                isRunning = false
            }
        }
        rc.done("test")
        delay(16)

        assertFalse(isRunning, "Job leaked after an immediate done")
    }

    @Test
    fun testStartRelaunchesCompletedJob() = runBlockingTest {
        var starts = 0
        var stops = 0
        val rc = RcCoroutine()

        rc.start("test") { starts++ }
        delay(16)

        val block: suspend () -> Unit = suspend {
            starts++
            try {
                awaitCancellation()
            } finally {
                stops++
            }
        }
        rc.start("test", block)
        delay(16)
        assertEquals(2, starts, "Completed job wasn't relaunched")

        rc.done("test")
        delay(16)
        assertEquals(0, stops, "Stopped while a reference was still held")

        rc.done("test")
        delay(16)
        assertEquals(1, stops, "Didn't stop")
    }

    @Test
    fun testRestartWaitsForPreviousJobCleanup() = runBlockingTest {
        val events = mutableListOf<String>()
        val rc = RcCoroutine()

        val block: suspend () -> Unit = suspend {
            events += "start"
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    delay(50)
                    events += "cleanup"
                }
            }
        }

        rc.start("test", block)
        delay(16)
        rc.done("test")
        rc.start("test", block)
        delay(100)

        assertEquals(listOf("start", "cleanup", "start"), events)

        rc.done("test")
        delay(100)
        assertEquals(listOf("start", "cleanup", "start", "cleanup"), events)
    }

    @Test
    fun testRestartWaitsForOriginalCleanupAfterInterruptedRestart() = runBlockingTest {
        val events = mutableListOf<String>()
        val rc = RcCoroutine()

        val block: suspend () -> Unit = suspend {
            events += "start"
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    delay(100)
                    events += "cleanup"
                }
            }
        }

        rc.start("test", block)
        delay(16)
        rc.done("test")
        rc.start("test", block)
        rc.done("test")
        rc.start("test", block)
        delay(200)

        assertEquals(listOf("start", "cleanup", "start"), events)
        rc.cancel("test")
    }
}
