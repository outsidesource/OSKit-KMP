package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual object KMPDispatchers {
    actual val IO = Dispatchers.IO
}
