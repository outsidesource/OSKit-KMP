package com.outsidesource.oskitkmp.lib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LazyComputedTest {
    @Test
    fun lazyComputedTest() {
        val foo = LazyComputed<Int, Int> { it + 2 }
        val test = foo(0)
        val test2 = foo(2)

        assertEquals(test, 2)
        assertEquals(test, test2)
    }

    @Test
    fun lazyComputedReset() {
        val foo = LazyComputed<Int, Int> { it + 2 }
        val test = foo(0)
        foo.reset()
        val test2 = foo(2)

        assertEquals(test2, 4)
        assertNotEquals(test, test2)
    }
}