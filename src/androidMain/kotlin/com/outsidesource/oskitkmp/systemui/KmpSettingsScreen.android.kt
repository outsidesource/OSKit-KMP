package com.outsidesource.oskitkmp.systemui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpSettingsScreen(private val context: Context) : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenError> {
        val res = when (type) {
            SettingsScreenType.App -> openAppSettings()
            SettingsScreenType.SystemSettings -> openSystemSettings()
            SettingsScreenType.Bluetooth -> openBluetoothSettings()
            SettingsScreenType.Location -> openLocationSettings()
        }

        return if (res is Outcome.Error && type != SettingsScreenType.App && fallbackToAppSettings) {
            openAppSettings()
        } else {
            res
        }
    }

    private fun openAppSettings(): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }

    private fun openBluetoothSettings(): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }

    private fun openLocationSettings(): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }

    private fun openSystemSettings(): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Outcome.Ok(Unit)
        } catch (e: Exception) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }
}
