package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.CoroutineDispatcher

expect object KMPDispatchers {
    val IO: CoroutineDispatcher
}
