package com.outsidesource.oskitkmp.capability

import android.Manifest
import android.content.pm.PackageManager

internal class MicrophoneKmpCapability : AndroidKmpCapability() {

    override val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override fun checkHardwareSupport(): Boolean =
        context?.activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) ?: false
}
