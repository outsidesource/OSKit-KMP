package com.outsidesource.oskitkmp.lib

class JvmKmpScreenWakeLock : IKmpScreenWakeLock {
    override suspend fun acquire() {}
    override suspend fun release() {}
}
