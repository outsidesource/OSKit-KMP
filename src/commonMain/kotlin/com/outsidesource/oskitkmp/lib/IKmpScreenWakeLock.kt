package com.outsidesource.oskitkmp.lib

/**
 * Disables the Screen Idle Timeout preventing the screen from sleeping
 *
 * Android: Supported
 * iOS: Supported
 * JVM: Not supported
 * Web: Supported on all browsers that support navigator.wakeLock
 */
interface IKmpScreenWakeLock {
    suspend fun acquire()
    suspend fun release()
}
