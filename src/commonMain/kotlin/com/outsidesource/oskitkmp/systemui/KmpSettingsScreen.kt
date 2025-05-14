package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.outcome.Outcome

interface IKmpSettingsScreenOpener {
    suspend fun open(type: SettingsScreenType, fallbackToAppSettings: Boolean = true):
        Outcome<Unit, KmpSettingsScreenError>
}

/**
 * Represents a cross-platform settings screen opener implementation.
 *
 * This class allows opening specific types of settings screens
 * defined by the `SettingsScreenType` enum, such as application settings,
 * system settings, Bluetooth settings, or location settings.
 * Depending on the platform's support and implementation, not all screen types
 * may be available.
 *
 * On platforms that do not support this functionality, the method will return
 * an error with `KmpSettingsScreenError.NotSupportedByThisPlatform`.
 *
 * Usage:
 * - Create platform-specifc instances of this class via DI or expect/actual helper and use in your common module
 *
 * Functions:
 * - `open`: Opens the specified settings screen type.
 *   - Arguments:
 *     - `type`: The type of settings screen to open.
 *     - `fallbackToAppSettings`: Specifies whether to fall back to the app settings
 *       when the requested type is not supported.
 *   - Returns:
 *     - `Outcome<Unit, KmpSettingsScreenError>` representing success or failure.
 */
expect class KmpSettingsScreen : IKmpSettingsScreenOpener {
    override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenError>
}

enum class SettingsScreenType {
    App,
    SystemSettings,
    Bluetooth,
    Location,
}

enum class KmpSettingsScreenError {
    NotSupportedByThisPlatform,
    InternalPlatformException,
}
