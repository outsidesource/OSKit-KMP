@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.systemui

import kotlinx.coroutines.await
import kotlin.js.Promise

class WasmKmpScreenWakeLock : IKmpScreenWakeLock {

    private var wakeLock: WakeLockSentinel? = null

    override suspend fun acquire() {
        try {
            wakeLock = requestWakeLock().await<WakeLockSentinel?>()
        } catch (t: Throwable) {
            // Ignore
        }
    }

    override suspend fun release() {
        try {
            wakeLock?.release()?.await<JsAny?>()
            wakeLock = null
        } catch (t: Throwable) {
            // Ignore
        }
    }
}

private fun requestWakeLock(): Promise<WakeLockSentinel?> = js(
    """{
        try {
            if (navigator["wakeLock"] === undefined) return Promise.resolve(null);
            return navigator.wakeLock.request("screen");
        } catch (err) {
            return Promise.resolve(null);
        }
    }""",
)

private external interface WakeLockSentinel : JsAny {
    val released: Boolean
    val type: String
    fun release(): Promise<JsAny?>
}
