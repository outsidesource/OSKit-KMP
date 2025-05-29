package com.outsidesource.oskitkmp.capability

import androidx.activity.ComponentActivity
import com.outsidesource.oskitkmp.outcome.Outcome
import com.outsidesource.oskitkmp.systemui.KmpSettingsScreenOpener
import com.outsidesource.oskitkmp.systemui.SettingsScreenType

actual class KmpCapabilityContext(
    var activity: ComponentActivity,
)

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability =
    BluetoothKmpCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability =
    LocationKmpCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(context: KmpCapabilityContext?): Outcome<Unit, Any> {
    try {
        return KmpSettingsScreenOpener(context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized))
            .open(SettingsScreenType.App)
    } catch (e: Exception) {
        return Outcome.Error(e)
    }
}
