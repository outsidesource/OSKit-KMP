package com.outsidesource.oskitkmp.capability

import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.systemui.KmpSettingsScreenOpener
import com.outsidesource.oskitkmp.systemui.SettingsScreenType

actual class KmpCapabilityContext()

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability =
    BluetoothKmpCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability =
    LocationKmpCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(
    context: KmpCapabilityContext?,
): Outcome<Unit, Any> {
    return if (context != null) {
        KmpSettingsScreenOpener.open(context, SettingsScreenType.App)
    } else {
        Outcome.Error(KmpCapabilitiesError.Uninitialized)
    }
}
