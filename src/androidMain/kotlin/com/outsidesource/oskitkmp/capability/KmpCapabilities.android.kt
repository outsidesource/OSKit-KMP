package com.outsidesource.oskitkmp.capability

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpCapabilityContext(
    var activity: ComponentActivity,
)

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): IKmpCapability =
    BluetoothKmpCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): IKmpCapability =
    LocationKmpCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(context: KmpCapabilityContext?): Outcome<Unit, Any> {
    try {
        val activity = context?.activity ?: return Outcome.Error(KmpCapabilitiesError.Uninitialized)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
        return Outcome.Ok(Unit)
    } catch (e: Exception) {
        return Outcome.Error(e)
    }
}
