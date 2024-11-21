package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome

actual class CapabilityContext()

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): ICapability =
    BluetoothCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): ICapability =
    LocationCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(
    context: CapabilityContext?,
): Outcome<Unit, Any> = Outcome.Error(CapabilityServiceError.UnsupportedOperation)
