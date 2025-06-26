package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.capability.KmpCapabilityContext
import com.outsidesource.oskitkmp.outcome.Outcome

actual object KmpSettingsScreenOpener : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        context: KmpCapabilityContext,
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenOpenerError> {
        return Outcome.Error(KmpSettingsScreenOpenerError.UnsupportedPlatform)
    }
}
