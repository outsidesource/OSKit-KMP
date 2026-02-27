package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A reference counted coroutine.
 *
 * Only one job is started per key. [start] may be called multiple times. [done] must be called the same number
 * of times as [start] in order for the job to be cancelled. Calling [cancel] will also cancel the job and
 * clear the key.
 *
 * Use this when you want to coalesce multiple “requests” under the same key into
 * a single running coroutine, and automatically tear it down once everyone’s
 * signaled they’re done.
 */
class RcCoroutine(
    private val scope: CoroutineScope = CoroutineScope(KmpDispatchers.Default + SupervisorJob()),
) {

    private data class Entry(val job: Job, val count: AtomicInt)

    private val mutex = Mutex()
    private val entries: MutableMap<Any, Entry> = mutableMapOf()

    fun start(key: Any, block: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                if (entries.containsKey(key)) {
                    entries[key]?.count?.incrementAndGet()
                    return@launch
                }
                entries[key] = Entry(job = scope.launch { block() }, count = atomic(1))
            }
        }
    }

    fun done(key: Any) {
        scope.launch {
            mutex.withLock {
                val entry = entries[key] ?: return@launch
                if (entry.count.decrementAndGet() > 0) return@launch
                entries.remove(key)?.job?.cancel()
            }
        }
    }

    fun cancel(key: Any) {
        scope.launch {
            mutex.withLock {
                entries.remove(key)?.job?.cancel()
            }
        }
    }
}
