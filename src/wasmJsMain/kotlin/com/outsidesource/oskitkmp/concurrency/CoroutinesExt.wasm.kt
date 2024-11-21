package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.*

actual object KMPDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.Default
    actual val Default: CoroutineDispatcher = Dispatchers.Default
    actual val Main: CoroutineDispatcher = Dispatchers.Main
    actual val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
