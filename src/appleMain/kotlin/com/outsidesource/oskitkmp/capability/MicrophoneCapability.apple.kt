package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted

internal class MicrophoneKmpCapability : IInitializableKmpCapability, IKmpCapability {

    private val internalStateFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

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

    override val status: Flow<CapabilityStatus> = flow {
        emit(queryStatus())
        emitAll(internalStateFlow.map { queryStatus() })
    }.distinctUntilChanged()

    private var hardwareSupportsCapability: Boolean = true
    private var hasRequestedPermissions: Boolean = false

    override fun init(context: KmpCapabilityContext) {}

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

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> = internalOpenAppSettingsScreen(null)
}
