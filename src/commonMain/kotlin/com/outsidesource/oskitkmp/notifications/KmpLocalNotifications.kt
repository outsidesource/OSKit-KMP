package com.outsidesource.oskitkmp.notifications

import com.outsidesource.oskitkmp.outcome.Outcome

expect class KmpLocalNotificationsContext

internal interface ILocalNotificationsContextScope {
    val context: KmpLocalNotificationsContext?
}

internal expect fun createPlatformNotificationManager(): IKmpLocalNotificationManager

class KmpLocalNotifications {
    private var context: KmpLocalNotificationsContext? = null

    val notificationManager: IKmpLocalNotificationManager = createPlatformNotificationManager()

    fun init(context: KmpLocalNotificationsContext) {
        this.context = context
        (notificationManager as? IInitializableKmpLocalNotificationManager)?.init(context)
    }
}

interface IInitializableKmpLocalNotificationManager : IKmpLocalNotificationManager {
    fun init(context: KmpLocalNotificationsContext)
}

interface IKmpLocalNotificationManager {
    fun configureChannel(id: String, name: String)
    suspend fun showNotification(data: NotificationData): Outcome<Unit, Any>
}

data class NotificationData(
    val id: String,
    val title: String,
    val message: String,
    val iconRes: Int? = null,
    val actions: List<NotificationAction> = emptyList(),
)

data class NotificationAction(
    val id: String,
    val title: String,
    val iconRes: Int? = null,
)
