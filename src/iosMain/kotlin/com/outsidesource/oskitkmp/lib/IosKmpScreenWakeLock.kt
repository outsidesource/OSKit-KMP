package com.outsidesource.oskitkmp.lib

import platform.UIKit.UIApplication

class IosKmpScreenWakeLock : IKmpScreenWakeLock {

    override suspend fun acquire() {
        UIApplication.sharedApplication.setIdleTimerDisabled(true)
    }

    override suspend fun release() {
        UIApplication.sharedApplication.setIdleTimerDisabled(false)
    }
}
