package com.outsidesource.oskitkmp.notifications

import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpLocalNotificationsContext

internal actual fun createPlatformNotificationManager(): IKmpLocalNotificationManager {
    return PlatformLocalNotificationManager()
}

class PlatformLocalNotificationManager : IInitializableKmpLocalNotificationManager {
    override fun init(context: KmpLocalNotificationsContext) {
        TODO("Not yet implemented")
    }

    override fun configureChannel(id: String, name: String) {
        TODO("Not yet implemented")
    }

    override suspend fun showNotification(data: NotificationData): Outcome<Unit, Any> {
        TODO("Not yet implemented")
    }
}
