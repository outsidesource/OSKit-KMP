package com.outsidesource.oskitkmp.capability

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.outsidesource.oskitkmp.outcome.Outcome

actual class CapabilityContext(
    var activity: ComponentActivity,
)

internal actual fun createPlatformBluetoothCapability(flags: Array<BluetoothCapabilityFlags>): ICapability =
    BluetoothCapability(flags)

internal actual fun createPlatformLocationCapability(flags: Array<LocationCapabilityFlags>): ICapability =
    LocationCapability(flags)

internal actual suspend fun internalOpenAppSettingsScreen(context: CapabilityContext?): Outcome<Unit, Any> {
    try {
        val activity = context?.activity ?: return Outcome.Error(CapabilityServiceError.Uninitialized)
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
        return Outcome.Ok(Unit)
    } catch (e: Exception) {
        return Outcome.Error(e)
    }
}
