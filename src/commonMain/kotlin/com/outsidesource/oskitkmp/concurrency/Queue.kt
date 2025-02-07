package com.outsidesource.oskitkmp.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class Queue {
    private val channel = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            for (task in channel) {
                try {
                    task.invoke()
                } catch (_: Throwable) {
                    // Do nothing
                }
            }
        }
    }

    fun enqueue(block: suspend () -> Unit) {
        channel.trySend(block)
    }
}
