package com.outsidesource.oskitkmp.settings

import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current

internal actual fun createPlatformHandler(context: SettingsContext): SettingsScreenHandler {
    return DesktopSettingsScreenHandler()
}

actual class SettingsContext

private class DesktopSettingsScreenHandler : SettingsScreenHandler {

    override suspend fun openSettings(type: SettingsScreenType): Boolean {
        return try {
            when(Platform.current) {
                Platform.Windows -> openWindowsSettings(type)
                Platform.MacOS -> openMacSettings(type)
                Platform.Linux -> openLinuxSettings(type)
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun openWindowsSettings(type: SettingsScreenType): Boolean {
        val command = when (type) {
            SettingsScreenType.App -> "ms-settings:"
            SettingsScreenType.Bluetooth -> "ms-settings:bluetooth"
            SettingsScreenType.Location -> "ms-settings:privacy-location"
            else -> return false
        }

        return try {
            Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", command))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openMacSettings(type: SettingsScreenType): Boolean {
        val command = when (type) {
            SettingsScreenType.App -> "tell application \"System Preferences\" to activate"
            SettingsScreenType.Bluetooth -> "tell application \"System Preferences\"\n" +
                    "reveal anchor \"Bluetooth\" of pane id \"com.apple.preferences.Bluetooth\"\n" +
                    "activate\n" +
                    "end tell"
            SettingsScreenType.Location -> "tell application \"System Preferences\"\n" +
                    "reveal anchor \"Privacy_LocationServices\" of pane id \"com.apple.preference.security\"\n" +
                    "activate\n" +
                    "end tell"
            else -> return false
        }

        return try {
            Runtime.getRuntime().exec(arrayOf("osascript", "-e", command))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openLinuxSettings(type: SettingsScreenType): Boolean {
        val command = when (type) {
            SettingsScreenType.App -> "gnome-control-center"
            SettingsScreenType.Bluetooth -> "gnome-control-center bluetooth"
            SettingsScreenType.Location -> "gnome-control-center privacy"
            else -> return false
        }

        return try {
            Runtime.getRuntime().exec(command)
            true
        } catch (_: Exception) {
            false
        }
    }
}