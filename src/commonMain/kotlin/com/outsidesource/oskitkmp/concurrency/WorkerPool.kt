package com.outsidesource.oskitkmp.concurrency

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*

class WorkerPool(
    private val workerCount: Int = 10,
    private val scope: CoroutineScope = CoroutineScope(IODispatcher + SupervisorJob())
) {

    private val jobCount = atomic(0)
    private val lock = SynchronizedObject()
    private val jobs: MutableList<Job> = mutableListOf()
    private val queue: MutableList<suspend () -> Unit> = mutableListOf()

    fun add(task: suspend () -> Unit) {
        if (jobCount.value >= workerCount) {
            synchronized(lock) {
                queue.add(task)
            }
            return
        }

        run(task)
    }

    fun cancel() {
        synchronized(lock) {
            queue.clear()
            jobs.forEach { it.cancel() }
        }
    }

    private fun enqueueNext() {
        val task = synchronized(lock) { queue.removeFirstOrNull() ?: return }
        run(task)
    }

    private fun run(task: suspend () -> Unit) {
        jobCount.incrementAndGet()

        val job = scope.launch {
            task()
        }.apply {
            invokeOnCompletion {
                jobCount.decrementAndGet()
                synchronized(lock) { jobs.remove(job) }
                enqueueNext()
            }
        }

        synchronized(lock) { jobs.add(job) }
    }
}
