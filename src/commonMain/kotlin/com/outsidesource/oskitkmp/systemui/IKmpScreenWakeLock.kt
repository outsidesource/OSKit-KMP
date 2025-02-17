package com.outsidesource.oskitkmp.systemui

/**
 * Disables the Screen Idle Timeout preventing the screen from sleeping
 *
 * Android: Supported
 * iOS: Supported
 * JVM: Not supported - Uses a no-op function
 * Web: Supported on all browsers that support navigator.wakeLock
 */
interface IKmpScreenWakeLock {
    suspend fun acquire()
    suspend fun release()
}
