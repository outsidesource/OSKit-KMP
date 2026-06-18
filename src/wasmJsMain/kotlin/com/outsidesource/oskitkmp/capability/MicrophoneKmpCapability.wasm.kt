@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class MicrophoneKmpCapability: IInitializableKmpCapability, IKmpCapability {
    private val localStatusFlow = MutableStateFlow<CapabilityStatus>(
        if (!hardwareSupportsCapability()) {
            CapabilityStatus.Unsupported()
        } else {
            CapabilityStatus.Unknown
        },
    )

    override val status: Flow<CapabilityStatus> = localStatusFlow

    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override fun init(context: KmpCapabilityContext) {
        scope.launch {
            if (!hardwareSupportsCapability()) return@launch

            val status = queryPermissions().unwrapOrReturn { return@launch }
            localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status)

            status.onchange = { localStatusFlow.value = mapJsPermissionStatusToCapabilityStatus(status) }
        }
    }

    override suspend fun queryStatus(): CapabilityStatus {
        if (!hardwareSupportsCapability()) return CapabilityStatus.Unsupported()

        val status = queryPermissions().unwrapOrReturn { return CapabilityStatus.Unknown }
        return mapJsPermissionStatusToCapabilityStatus(status)
    }

    private suspend fun queryPermissions(): Outcome<PermissionStatus, Any> = navigator.permissions
        .query(permissionQueryParams("microphone"))
        .kmpAwaitOutcome()

    private fun mapJsPermissionStatusToCapabilityStatus(status: PermissionStatus): CapabilityStatus {
        return when (status.state) {
            "granted" -> CapabilityStatus.Ready
            "prompt" -> CapabilityStatus.NoPermission(reason = NoPermissionReason.NotRequested)
            else -> CapabilityStatus.NoPermission(reason = NoPermissionReason.DeniedPermanently)
        }
    }

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        navigator.mediaDevices.getUserMedia(requestQueryParams()).kmpAwaitOutcome().unwrapOrReturn {
            localStatusFlow.value = CapabilityStatus.NoPermission(NoPermissionReason.DeniedPermanently)
            return Outcome.Ok(localStatusFlow.value)
        }

        localStatusFlow.value = CapabilityStatus.Ready
        return Outcome.Ok(CapabilityStatus.Ready)
    }

    override suspend fun requestEnable(): Outcome<CapabilityStatus, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openServiceSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)

    override suspend fun openAppSettingsScreen(): Outcome<Unit, Any> =
        Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
}


private fun permissionQueryParams(name: String): JsAny = js("""({ name: name })""")
private fun requestQueryParams(): JsAny = js("""({ audio: true })""")

private fun hardwareSupportsCapability(): Boolean = js("""navigator.mediaDevices !== undefined""")
