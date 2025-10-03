package org.godotengine.plugin.android.localnotificationscheduler


import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.util.Log


class NotificationReceiver : BroadcastReceiver() {
    private val notificationHandler = NotificationHandler()

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val info = notificationHandler.getNotification(context, id)

        val channelId = (info["channelId"] ?: "default") as String
        val title = (info["title"] ?: "Reminder") as String
        val text = (info["text"] ?: "") as String

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
        notificationHandler.addNotificationSettup(context, id, builder)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())

        Log.d(Constants.LOG_TAG, "Delivered scheduled notification: $title - $text")

        notificationHandler.refreshScheduleWeeklyNotification(context, id)
    }
}
