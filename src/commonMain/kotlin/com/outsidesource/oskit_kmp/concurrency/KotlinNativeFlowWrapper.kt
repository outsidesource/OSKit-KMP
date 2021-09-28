package com.outsidesource.oskit_kmp.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

class KotlinNativeFlowWrapper<T: Any>(private val flow: Flow<T>) {
    fun subscribe(
        scope: CoroutineScope,
        onEach: (item: T) -> Unit,
        onComplete: () -> Unit,
        onThrow: (error: Throwable) -> Unit
    ) = flow
        .onEach { onEach(it) }
        .catch { onThrow(it) }
        .onCompletion { onComplete() }
        .launchIn(scope)
}