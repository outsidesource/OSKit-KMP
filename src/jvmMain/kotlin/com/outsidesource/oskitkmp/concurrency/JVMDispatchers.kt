package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.Dispatchers

actual object KMPDispatchers {
    actual val IO = Dispatchers.IO
}
