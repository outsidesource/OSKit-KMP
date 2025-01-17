package com.outsidesource.oskitkmp.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

actual class SettingsContext(val context: Context)

actual fun createPlatformHandler(context: SettingsContext): SettingsScreenHandler {
    return AndroidSettingsScreenHandler(context.context)
}

private class AndroidSettingsScreenHandler(private val context: Context) : SettingsScreenHandler {
    override suspend fun openSettings(type: SettingsScreenType): Boolean {
        return when (type) {
            SettingsScreenType.App -> openAppSettings()
            SettingsScreenType.Bluetooth -> openBluetoothSettings()
            SettingsScreenType.Location -> openLocationSettings()
        }
    }

    private fun openAppSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openBluetoothSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openLocationSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}