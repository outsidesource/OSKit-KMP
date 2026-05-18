package com.outsidesource.oskitkmp.systemui

import kotlinx.cinterop.*
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.IOKit.*

@OptIn(ExperimentalForeignApi::class)
class MacOsKmpScreenWakeLock : IKmpScreenWakeLock {

    private var assertionID: IOPMAssertionIDVar = nativeHeap.alloc<IOPMAssertionIDVar>()
    private var isHeld = false

    override suspend fun acquire() {
        if (isHeld) return

        val type = kIOPMAssertionTypeNoIdleSleep as CFStringRef
        val level = kIOPMAssertionLevelOn
        val reasonStr = CFStringCreateWithCString(null, "OsKitKmp", kCFStringEncodingUTF8)

        val result = IOPMAssertionCreateWithName(
            type,
            level,
            reasonStr,
            assertionID.ptr
        )

        if (result == kIOReturnSuccess) isHeld = true
    }

    override suspend fun release() {
        if (!isHeld) return
        if (IOPMAssertionRelease(assertionID.value) == kIOReturnSuccess) isHeld = false
    }
}
