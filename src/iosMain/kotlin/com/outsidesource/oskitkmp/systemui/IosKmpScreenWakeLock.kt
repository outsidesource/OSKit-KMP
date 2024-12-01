package com.outsidesource.oskitkmp.systemui

import platform.UIKit.UIApplication

class IosKmpScreenWakeLock : IKmpScreenWakeLock {

    override suspend fun acquire() {
        UIApplication.Companion.sharedApplication.setIdleTimerDisabled(true)
    }

    override suspend fun release() {
        UIApplication.Companion.sharedApplication.setIdleTimerDisabled(false)
    }
}
