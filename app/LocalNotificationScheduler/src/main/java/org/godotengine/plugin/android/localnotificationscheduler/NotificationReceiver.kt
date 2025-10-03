package org.godotengine.plugin.android.localnotificationscheduler


import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_MAX
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

        val pendingNotificationIntent = notificationHandler.getPendingNotificationIntent(context, id)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(IMPORTANCE_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingNotificationIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())

        Log.d(Constants.LOG_TAG, "Delivered scheduled notification: $title - $text")

        notificationHandler.refreshScheduleWeeklyNotification(context, id)
    }
}
