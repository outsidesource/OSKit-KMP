package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.lib.Platform
import com.outsidesource.oskitkmp.lib.current
import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpSettingsScreen : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            when (Platform.current) {
                Platform.Windows -> openWindowsSettings(type)
                Platform.MacOS -> openMacSettings(type)
                Platform.Linux -> openLinuxSettings(type)
                else -> Outcome.Error(KmpSettingsScreenError.NotSupportedByThisPlatform)
            }
        } catch (_: Exception) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }

    private fun openWindowsSettings(type: SettingsScreenType): Outcome<Unit, KmpSettingsScreenError> {
        return when (type) {
            SettingsScreenType.App -> Outcome.Error(KmpSettingsScreenError.NotSupportedByThisPlatform)
            SettingsScreenType.SystemSettings -> try {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "ms-settings:"))
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }

            SettingsScreenType.Bluetooth -> try {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "ms-settings:bluetooth"))
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }

            SettingsScreenType.Location -> try {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "ms-settings:privacy-location"))
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }
        }
    }

    private fun openMacSettings(type: SettingsScreenType): Outcome<Unit, KmpSettingsScreenError> {
        return when (type) {
            SettingsScreenType.App -> Outcome.Error(KmpSettingsScreenError.NotSupportedByThisPlatform)
            SettingsScreenType.SystemSettings -> try {
                Runtime.getRuntime()
                    .exec(arrayOf("osascript", "-e", "tell application \"System Preferences\" to activate"))
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }

            SettingsScreenType.Bluetooth -> try {
                Runtime.getRuntime().exec(
                    arrayOf(
                        "osascript", "-e",
                        "tell application \"System Preferences\"\n" +
                                "reveal anchor \"Bluetooth\" of pane id \"com.apple.preferences.Bluetooth\"\n" +
                                "activate\n" +
                                "end tell",
                    ),
                )
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }

            SettingsScreenType.Location -> try {
                Runtime.getRuntime().exec(
                    arrayOf(
                        "osascript", "-e",
                        "tell application \"System Preferences\"\n" +
                                "reveal anchor \"Privacy_LocationServices\" of pane id \"com.apple.preference.security\"\n" +
                                "activate\n" +
                                "end tell",
                    ),
                )
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }
        }
    }

    //TODO: Detect and support more desktop envs like KDE, etc....
    private fun openLinuxSettings(type: SettingsScreenType): Outcome<Unit, KmpSettingsScreenError> {
        return when (type) {
            SettingsScreenType.App -> Outcome.Error(KmpSettingsScreenError.NotSupportedByThisPlatform)
            SettingsScreenType.SystemSettings -> try {
                Runtime.getRuntime().exec("gnome-control-center")
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }

            SettingsScreenType.Bluetooth -> try {
                Runtime.getRuntime().exec("gnome-control-center bluetooth")
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }

            SettingsScreenType.Location -> try {
                Runtime.getRuntime().exec("gnome-control-center privacy")
                Outcome.Ok(Unit)
            } catch (_: Exception) {
                Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
            }
        }
    }
}