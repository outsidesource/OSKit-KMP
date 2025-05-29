package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.outcome.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class KmpSettingsScreenOpener : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenOpenerError> {
        val res = when (type) {
            SettingsScreenType.App -> openAppSettings()
            else -> Outcome.Error(KmpSettingsScreenOpenerError.UnsupportedPlatform)
        }

        return if (res is Outcome.Error && type != SettingsScreenType.App && fallbackToAppSettings) {
            openAppSettings()
        } else {
            res
        }
    }

    private suspend fun openAppSettings(): Outcome<Unit, KmpSettingsScreenOpenerError> {
        return withContext(Dispatchers.Main) {
            try {
                val url = NSURL(string = UIApplicationOpenSettingsURLString)
                if (UIApplication.sharedApplication.canOpenURL(url)) {
                    UIApplication.sharedApplication.openURL(url)
                    Outcome.Ok(Unit)
                } else {
                    Outcome.Error(KmpSettingsScreenOpenerError.UnsupportedPlatform)
                }
            } catch (e: Throwable) {
                Outcome.Error(KmpSettingsScreenOpenerError.Unknown)
            }
        }
    }

}
