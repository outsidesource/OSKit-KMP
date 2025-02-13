package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object KmpDispatchers {
    actual val IO: CoroutineDispatcher = Dispatchers.IO
    actual val Default: CoroutineDispatcher = Dispatchers.Default
    actual val Main: CoroutineDispatcher = Dispatchers.Main
    actual val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
