package com.outsidesource.oskitkmp.lib

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

class Deferrer {
    private val deferred = atomic<List<() -> Unit>>(emptyList())

    fun defer(block: () -> Unit): Deferrer {
        deferred.update { it + block }
        return this
    }

    fun run() {
        val blocks = deferred.value
        for (i in blocks.indices.reversed()) {
            blocks[i]()
        }
        deferred.update { emptyList() }
    }
}

class SuspendDeferrer {
    private val deferred = atomic<List<suspend () -> Unit>>(emptyList())

    fun defer(block: suspend () -> Unit): SuspendDeferrer {
        deferred.update { it + block }
        return this
    }

    suspend fun run() {
        val blocks = deferred.value
        for (i in blocks.indices.reversed()) {
            blocks[i]()
        }
        deferred.update { emptyList() }
    }
}

inline fun <T> withDefer(block: (defer: (() -> Unit) -> Unit) -> T): T {
    val deferrer = Deferrer()

    return try {
        block(deferrer::defer)
    } finally {
        deferrer.run()
    }
}

suspend inline fun <T> withSuspendDefer(block: (defer: (suspend () -> Unit) -> Unit) -> T): T {
    val deferrer = SuspendDeferrer()

    return try {
        block(deferrer::defer)
    } finally {
        deferrer.run()
    }
}

suspend inline fun <T> coroutineScopeWithDefer(
    crossinline block: suspend CoroutineScope.(defer: (() -> Unit) -> Unit) -> T,
): T = withDefer { defer ->
    coroutineScope {
        block(defer)
    }
}

suspend inline fun <T> coroutineScopeWithSuspendDefer(
    crossinline block: suspend CoroutineScope.(defer: (suspend () -> Unit) -> Unit) -> T,
): T = withSuspendDefer { defer ->
    coroutineScope {
        block(defer)
    }
}

suspend inline fun <T> flowWithDefer(
    crossinline block: suspend FlowCollector<T>.(defer: (() -> Unit) -> Unit) -> Unit,
): Flow<T> = flow {
    withDefer { defer ->
        block(defer)
    }
}

suspend inline fun <T> flowWithSuspendDefer(
    crossinline block: suspend FlowCollector<T>.(defer: (suspend () -> Unit) -> Unit) -> Unit,
): Flow<T> = flow {
    withSuspendDefer { defer ->
        block(defer)
    }
}

suspend inline fun <T> channelFlowWithDefer(
    crossinline block: suspend ProducerScope<T>.(defer: (() -> Unit) -> Unit) -> Unit,
): Flow<T> = channelFlow {
    withDefer { defer ->
        block(defer)
    }
}

suspend inline fun <T> channelFlowWithSuspendDefer(
    crossinline block: suspend ProducerScope<T>.(defer: (suspend () -> Unit) -> Unit) -> Unit,
): Flow<T> = channelFlow {
    withSuspendDefer { defer ->
        block(defer)
    }
}
