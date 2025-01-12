package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome

actual class CapabilityContext()

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability =
    BluetoothKmpCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability =
    LocationKmpCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(
    context: CapabilityContext?,
): Outcome<Unit, Any> = Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
