package com.outsidesource.oskitkmp.settings

interface SettingsScreenHandler {
    suspend fun openSettings(type: SettingsScreenType): Boolean
}

enum class SettingsScreenType {
    App,
    Bluetooth,
    Location,
}

expect class SettingsContext

internal expect fun createPlatformHandler(context: SettingsContext): SettingsScreenHandler

object SettingsScreen {
    private lateinit var handler: SettingsScreenHandler

    fun initialize(context: SettingsContext) {
        handler = createPlatformHandler(context)
    }

    suspend fun open(
        type: SettingsScreenType = SettingsScreenType.App,
        fallbackToAppSettings: Boolean = true
    ): Boolean {
        if (!::handler.isInitialized) {
            throw IllegalStateException("SettingsScreen is not initialized. Call initialize(context) first.")
        }

        return if (!handler.openSettings(type) && fallbackToAppSettings) {
            handler.openSettings(SettingsScreenType.App)
        } else {
            true
        }
    }
}