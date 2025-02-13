package com.outsidesource.oskitkmp.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

fun runBlockingTest(block: suspend CoroutineScope.() -> Unit) = runTest {
    withContext(Dispatchers.Default) {
        block()
    }
}