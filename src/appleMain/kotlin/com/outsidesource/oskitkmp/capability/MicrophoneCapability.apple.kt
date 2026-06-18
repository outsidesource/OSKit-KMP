package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted

internal class MicrophoneKmpCapability : AppleKmpCapability() {

    private val microphoneManager by lazy {
        runBlocking(Dispatchers.Main) {
            AVAudioApplication.sharedInstance()
        }
    }

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = true
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun queryStatus(): CapabilityStatus {
        if (!hardwareSupportsCapability) return CapabilityStatus.Unsupported()

        val hasAuthorization = microphoneManager.recordPermission == AVAudioApplicationRecordPermissionGranted

        if (!hasAuthorization) {
            val reason = if (microphoneManager.recordPermission == AVAudioApplicationRecordPermissionDenied) {
                NoPermissionReason.DeniedPermanently
            } else {
                NoPermissionReason.NotRequested
            }
            return CapabilityStatus.NoPermission(reason)
        }

        return CapabilityStatus.Ready
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        suspendCancellableCoroutine { continuation ->
            AVAudioApplication.requestRecordPermissionWithCompletionHandler {
                continuation.resume(it) { _, _, _ -> }
            }
        }
        hasRequestedPermissions = true
        internalStateFlow.emit(Unit)
        return Outcome.Ok(queryStatus())
    }
}
