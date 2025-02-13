package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpCapabilityContext()

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability =
    BluetoothKmpCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability =
    LocationKmpCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(
    context: KmpCapabilityContext?,
): Outcome<Unit, Any> = Outcome.Error(KmpCapabilitiesError.UnsupportedOperation)
