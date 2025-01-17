package com.outsidesource.oskitkmp.settings

actual class SettingsContext

actual fun createPlatformHandler(context: SettingsContext): SettingsScreenHandler {
    return IOSSettingsScreenHandler()
}

private class IOSSettingsScreenHandler : SettingsScreenHandler {
    override suspend fun openSettings(type: SettingsScreenType): Boolean {
        val urlString = when (type) {
            SettingsScreenType.App -> "App-prefs:"
            SettingsScreenType.Bluetooth -> "App-prefs:Bluetooth"
            SettingsScreenType.Location -> "App-prefs:Privacy&path=LOCATION"
            // Добавьте другие типы настроек, если требуется
            else -> null
        }

        return if (urlString != null) {
            val url = NSURL(string = urlString)
            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url)
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}