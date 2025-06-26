package com.outsidesource.oskitkmp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import com.outsidesource.oskitkmp.outcome.Outcome

actual class KmpLocalNotificationsContext(
    var activity: ComponentActivity,
)

internal actual fun createPlatformNotificationManager(): IKmpLocalNotificationManager {
    return AndroidLocalNotificationManager()
}

class AndroidLocalNotificationManager : IInitializableKmpLocalNotificationManager {

    private var context: Context? = null
    private var channelId: String = "default_channel"
    private var channelName: String = "Default Channel"

    override fun init(context: KmpLocalNotificationsContext) {
        this.context = context.activity
        createNotificationChannel()
    }

    override fun configureChannel(id: String, name: String) {
        this.channelId = id
        this.channelName = name
        createNotificationChannel()
    }

    override suspend fun showNotification(data: NotificationData): Outcome<Unit, Any> {
        val ctx = context ?: return Outcome.Error(Unit)

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(ctx, channelId)
            .setContentTitle(data.title)
            .setContentText(data.message)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setAutoCancel(true)

        val mainIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val mainPendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        builder.setContentIntent(mainPendingIntent)

        data.actions.forEach { action ->
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                action.title,
                mainPendingIntent,
            )
        }

        manager.notify(data.id.hashCode(), builder.build())
        return Outcome.Ok(Unit)
    }

    private fun createNotificationChannel() {
        val ctx = context ?: return
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(channel)
    }
}
