package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpSettingsScreenOpener : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenOpenerError> {
        return Outcome.Error(KmpSettingsScreenOpenerError.UnsupportedPlatform)
    }
}
