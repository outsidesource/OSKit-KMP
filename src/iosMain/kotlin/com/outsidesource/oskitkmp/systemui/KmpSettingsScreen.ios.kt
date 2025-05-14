package com.outsidesource.oskitkmp.systemui

import com.outsidesource.oskitkmp.outcome.Outcome
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual class KmpSettingsScreen : IKmpSettingsScreenOpener {
    actual override suspend fun open(
        type: SettingsScreenType,
        fallbackToAppSettings: Boolean,
    ): Outcome<Unit, KmpSettingsScreenError> {
        val res = when (type) {
            SettingsScreenType.App -> openAppSettings()
            SettingsScreenType.SystemSettings -> launchUrl("App-prefs:")
            SettingsScreenType.Bluetooth -> launchUrl("App-prefs:Bluetooth")
            SettingsScreenType.Location -> launchUrl("App-prefs:Privacy&path=LOCATION")
        }

        return if (res is Outcome.Error && type != SettingsScreenType.App && fallbackToAppSettings) {
            openAppSettings()
        } else {
            res
        }
    }

    private fun openAppSettings(): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            val settingsUrl: NSURL = NSURL.URLWithString(UIApplicationOpenSettingsURLString)!!
            UIApplication.sharedApplication.openURL(settingsUrl, emptyMap<Any?, Any>(), null)
            Outcome.Ok(Unit)
        } catch (e: Throwable) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }

    private fun launchUrl(urlString: String): Outcome<Unit, KmpSettingsScreenError> {
        return try {
            val url = NSURL(string = urlString)
            if (UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url)
                Outcome.Ok(Unit)
            } else {
                Outcome.Error(KmpSettingsScreenError.NotSupportedByThisPlatform)
            }
        } catch (_: Throwable) {
            Outcome.Error(KmpSettingsScreenError.InternalPlatformException)
        }
    }
}
