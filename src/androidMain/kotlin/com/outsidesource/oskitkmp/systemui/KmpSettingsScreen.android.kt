package com.outsidesource.oskitkmp.systemui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpSettingsScreenOpener(private val context: Context) : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenOpenerError> {
        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }

        val res = launchIntent(
            context,
            when (type) {
                SettingsScreenType.App -> appSettingsIntent
                SettingsScreenType.SystemSettings -> Intent(Settings.ACTION_SETTINGS)
                SettingsScreenType.Bluetooth -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                SettingsScreenType.Location -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            },
        )

        return if (res is Outcome.Error && type != SettingsScreenType.App && fallbackToAppSettings) {
            launchIntent(context, appSettingsIntent)
        } else {
            res
        }
    }

    private fun launchIntent(context: Context, intent: Intent): Outcome<Unit, KmpSettingsScreenOpenerError> {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(KmpSettingsScreenOpenerError.Unknown)
        }
    }
}
