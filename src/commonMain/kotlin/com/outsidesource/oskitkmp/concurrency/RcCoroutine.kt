package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A reference counted coroutine.
 *
 * Only one job is started per key. [start] may be called multiple times. [done] must be called the same number
 * of times as [start] in order for the job to be cancelled. Calling [cancel] will also cancel the job and
 * clear the key.
 *
 * If a key is started again while its previous job is still finishing (i.e. running cleanup after cancellation), the
 * new job waits for the previous one to complete before running. If the job completes on its own while references are
 * still held, the next [start] for that key launches a new job and keeps the existing references.
 *
 * Use this when you want to coalesce multiple “requests” under the same key into
 * a single running coroutine, and automatically tear it down once everyone’s
 * signaled they’re done.
 */
class RcCoroutine(
    private val scope: CoroutineScope = CoroutineScope(KmpDispatchers.Default + SupervisorJob()),
) {

    private data class Entry(val job: Job, val count: Int)

    // Bookkeeping is synchronous so calls are applied in the order they are made. Launching a coroutine per call
    // let a done() overtake the start() it was meant to balance, which left the job running with nothing to stop it.
    private val lock = SynchronizedObject()

    // A key whose job is cancelled stays here with a count of 0 until that job completes, so a restart can wait for it.
    private val entries: MutableMap<Any, Entry> = mutableMapOf()

    fun start(key: Any, block: suspend () -> Unit) {
        synchronized(lock) {
            val entry = entries[key]
            if (entry == null) {
                entries[key] = Entry(job = scope.launch { block() }, count = 1)
                return
            }

            if (entry.count > 0 && !entry.job.isCompleted) {
                entries[key] = entry.copy(count = entry.count + 1)
                return
            }

            val previousJob = entry.job
            // This job must not complete before the previous one does, even if it is stopped while waiting. Otherwise a
            // later restart would wait on this already-completed job and skip the previous job's cleanup. ATOMIC makes
            // the body run even when cancelled before dispatch, and the join itself is non-cancellable.
            val job = scope.launch(start = CoroutineStart.ATOMIC) {
                withContext(NonCancellable) { previousJob.join() }
                ensureActive()
                block()
            }
            entries[key] = Entry(job = job, count = entry.count + 1)
        }
    }

    fun done(key: Any) {
        val stoppedJob = synchronized(lock) {
            val entry = entries[key] ?: return
            if (entry.count == 0) return
            if (entry.count > 1) {
                entries[key] = entry.copy(count = entry.count - 1)
                return
            }
            entries[key] = entry.copy(count = 0)
            entry.job
        }
        stop(key, stoppedJob)
    }

    fun cancel(key: Any) {
        val stoppedJob = synchronized(lock) {
            val entry = entries[key] ?: return
            entries[key] = entry.copy(count = 0)
            entry.job
        }
        stop(key, stoppedJob)
    }

    private fun stop(key: Any, job: Job) {
        job.cancel()
        // Registered outside the lock because the handler runs immediately if the job has already completed
        job.invokeOnCompletion {
            synchronized(lock) {
                val entry = entries[key]
                if (entry?.job === job && entry.count == 0) entries.remove(key)
            }
        }
    }
}
