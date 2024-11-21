package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual object KMPDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
    actual val Default: CoroutineDispatcher = Dispatchers.Default
    actual val Main: CoroutineDispatcher = Dispatchers.Main
    actual val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
