package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpSettingsScreen : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean
    ): Outcome<Unit, KmpSettingsScreenError> {
        return Outcome.Error(KmpSettingsScreenError.NotSupportedByThisPlatform)
    }
}