@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.concurrency.kmpAwaitOutcome
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.outcome.unwrapOrReturn

internal class MicrophoneKmpCapability : WasmPermissionKmpCapability(
    permissionName = "microphone",
    hardwareSupported = hardwareSupportsCapability(),
) {
    override val hasPermissions: Boolean = true
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> {
        val stream = navigator.mediaDevices.getUserMedia(requestQueryParams()).kmpAwaitOutcome().unwrapOrReturn {
            localStatusFlow.value = CapabilityStatus.NoPermission(NoPermissionReason.DeniedPermanently)
            return Outcome.Ok(localStatusFlow.value)
        }

        // getUserMedia opens a live capture stream solely to prompt for permission; stop its tracks
        // immediately so the microphone is released and the browser's "in use" indicator clears.
        stopStreamTracks(stream)

        localStatusFlow.value = CapabilityStatus.Ready
        return Outcome.Ok(CapabilityStatus.Ready)
    }
}

private fun requestQueryParams(): JsAny = js("""({ audio: true })""")
private fun stopStreamTracks(stream: JsAny): Unit = js("""stream.getTracks().forEach(function (t) { t.stop(); })""")
private fun hardwareSupportsCapability(): Boolean = js("""navigator.mediaDevices !== undefined""")
