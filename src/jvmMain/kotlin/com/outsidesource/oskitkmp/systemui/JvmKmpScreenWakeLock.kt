package com.outsidesource.oskitkmp.systemui

class JvmKmpScreenWakeLock : IKmpScreenWakeLock {
    override suspend fun acquire() {}
    override suspend fun release() {}
}
