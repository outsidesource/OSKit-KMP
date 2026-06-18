@file:OptIn(ExperimentalWasmJsInterop::class)

package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class LocationKmpCapability(
    flags: Array<LocationCapabilityFlags>,
) : WasmPermissionKmpCapability(
    permissionName = "geolocation",
    hardwareSupported = hardwareSupportsCapability(),
    isCapabilityRequired = when (flags.size) {
        1 -> flags[0] != LocationCapabilityFlags.BluetoothAccess
        else -> true
    },
) {
    override val hasPermissions: Boolean = isCapabilityRequired
    override val hasEnablableService: Boolean = false
    override val supportsRequestEnable: Boolean = false
    override val supportsOpenAppSettingsScreen: Boolean = false
    override val supportsOpenServiceSettingsScreen: Boolean = false

    override suspend fun requestPermissions(): Outcome<CapabilityStatus, Any> = suspendCoroutine { continuation ->
        navigator.geolocation.getCurrentPosition(
            success = {
                localStatusFlow.value = CapabilityStatus.Ready
                continuation.resume(Outcome.Ok(CapabilityStatus.Ready))
            },
            error = {
                if (it.code == 1) {
                    localStatusFlow.value = CapabilityStatus.NoPermission(NoPermissionReason.DeniedPermanently)
                    continuation.resume(Outcome.Ok(localStatusFlow.value))
                    return@getCurrentPosition
                }
                continuation.resume(Outcome.Error(KmpCapabilitiesError.Unknown))
            },
        )
    }
}

private fun hardwareSupportsCapability(): Boolean = js("""navigator.geolocation !== undefined""")
