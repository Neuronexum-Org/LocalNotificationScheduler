package org.godotengine.plugin.android.localnotificationscheduler

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit


class NotificationHandler {
    private fun saveNotification(
        context : Context,
        id: Int,
        channelId: String,
        title: String,
        text: String,
        priority: Int,
        autoCancel: Boolean,
        triggerAtMillis: Long,
        intervalMillis: Long?
    ) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val ids = prefs.getStringSet(Constants.NOTIF_ID_NAME, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(id.toString())

        prefs.edit {
            putString("notif_${id}_channelId", channelId)
            putString("notif_${id}_title", title)
            putString("notif_${id}_text", text)
            putInt("notif_${id}_priority", priority)
            putBoolean("notif_${id}_autoCancel", autoCancel)
            putLong("notif_${id}_trigger", triggerAtMillis)

            if (intervalMillis != null) {
                putLong("notif_${id}_interval", intervalMillis)
            } else {
                remove("notif_${id}_interval")
            }

            putStringSet(Constants.NOTIF_ID_NAME,ids)

            apply()
        }
    }

    private fun removeNotification(
        context : Context,
        id: Int
    ) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        var ids : Set<String> = prefs.getStringSet(Constants.NOTIF_ID_NAME,  setOf<String>())!!
        ids = ids.minus(id.toString())

        prefs.edit {
            remove("notif_${id}_channel")
            remove("notif_${id}_title")
            remove("notif_${id}_text")
            remove("notif_${id}_priority")
            remove("notif_${id}_autoCancel")
            remove("notif_${id}_trigger")
            remove("notif_${id}_interval")

            putStringSet(Constants.NOTIF_ID_NAME,ids)

            apply()
        }
    }


    fun requestNotificationPermission(
        activity : Activity?
    ): Boolean {
        val activity = activity ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    Constants.REQUEST_NOTIF_PERMISSION
                )

                Log.d(Constants.LOG_TAG, "Notification permission requested.")
                return false
            }

            Log.d(Constants.LOG_TAG, "Notification permission already granted.")
            return true
        }

        Log.d(Constants.LOG_TAG, "No runtime permission needed (API < 33).")
        return true
    }

    fun hasNotificationPermission(
        context : Context?
    ): Boolean {
        val context = context ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On Android 12 and below, permission is implicitly granted
            true
        }
    }


    fun createNotificationChannel(
        context : Context?,
        channelId : String
    ) {
        val context = context ?: return

        Log.d(Constants.LOG_TAG, "Notification channel created.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Godot Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Basic test channel"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(
        context : Context?,
        id: Int,
        channelId: String,
        title: String,
        text: String,
        priority : Int,
        autoCancel: Boolean,
        trigger: Long,
        interval: Long?
    ) {
        val context = context ?: return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("channelId", channelId)
            putExtra("title", title)
            putExtra("text", text)
            putExtra("priority", priority)
            putExtra("autoCancel", autoCancel)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger,
            pendingIntent
        )

        saveNotification(
            context,
            id,
            channelId,
            title,
            text,
            priority,
            autoCancel,
            trigger,
            interval
        )

        Log.d(
            Constants.LOG_TAG,
            "Schedule Notification at triggerAtMillis '$trigger'"
        )
    }

    fun scheduleInstantNotification(
        context : Context?,
        id: Int,
        channelId: String,
        title: String,
        text: String,
        priority : Int,
        autoCancel: Boolean
    ) {
        val context = context ?: return

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.btn_star)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setAutoCancel(autoCancel)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
        Log.d(Constants.LOG_TAG, "Notification requested: $title - $text")
    }

    fun cancelScheduledNotification(
        context : Context?,
        id: Int
    ) {
        val context = context ?: return

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        removeNotification(context, id)

        Log.d(Constants.LOG_TAG, "Notification, with id $id, was canceled.")
    }
}