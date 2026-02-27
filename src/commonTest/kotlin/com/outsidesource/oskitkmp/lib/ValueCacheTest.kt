package com.outsidesource.oskitkmp.lib

import com.outsidesource.oskitkmp.test.runBlockingTest
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueCacheTest {
    @Test
    fun testValueCacheSingleCompute() = runBlockingTest {
        var computeRun = 0
        val cache = ValueCache<Int>()

        for (i in 0..< 100) {
            cache {
                computeRun++
                1
            }
        }

        assertEquals(computeRun, 1, "Compute ran more than once")
        cache.reset()
        cache { computeRun++ }
        assertEquals(computeRun, 2, "Reset didn't work")
    }

    @Test
    fun testValueCacheRecompute() = runBlockingTest {
        var computeRun = 0
        val cache = ValueCache<Int>()

        for (i in 0..< 100) {
            cache(i) {
                computeRun++
                1
            }
        }

        assertEquals(computeRun, 100, "Compute did not run the proper amount of times")
    }

    @Test
    fun testConcurrentValueCacheValue() = runBlockingTest {
        val cache = ConcurrentValueCache<Int>()

        launch {
            for (i in 0..< 1000) {
                val value = cache(0) { 2 }
                assertEquals(value, 2, "Value did not equal expected")
            }
        }

        launch {
            for (i in 1000..< 2000) {
                val value = cache(10) { 12 }
                assertEquals(value, 12, "Value did not equal expected")
            }
        }
    }
}