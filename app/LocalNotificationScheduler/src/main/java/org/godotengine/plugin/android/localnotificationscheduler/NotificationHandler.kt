package org.godotengine.plugin.android.localnotificationscheduler

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_MAX
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
        // NOTIFICATION DATA SAVE/ACCESS
    private fun saveNotification(
        context : Context,
        id: Int,
        channelId: String,
        title: String,
        text: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>
    ) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        val ids = prefs.getStringSet(Constants.NOTIF_ID_NAME, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(id.toString())

        prefs.edit {
            putString("notif_${id}_channelId", channelId)
            putString("notif_${id}_title", title)
            putString("notif_${id}_text", text)
            putInt("notif_${id}_hour", hour)
            putInt("notif_${id}_minute", minute)
            putStringSet("notif_${id}_daysOfWeek", daysOfWeek.map { it.toString() }.toSet())

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
            remove("notif_${id}_channelId")
            remove("notif_${id}_title")
            remove("notif_${id}_text")
            remove("notif_${id}_hour")
            remove("notif_${id}_minute")
            remove("notif_${id}_daysOfWeek")

            putStringSet(Constants.NOTIF_ID_NAME,ids)

            apply()
        }
    }

    fun getNotification(
        context : Context,
        id: Int
    ): Map<String, Any> {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        if (
            !prefs.contains(Constants.NOTIF_ID_NAME) ||
            !prefs.getStringSet(Constants.NOTIF_ID_NAME,  setOf<String>())!!.contains(id.toString())
            ) {

            Log.d(Constants.LOG_TAG, "Attempted to get info from unassigned notification id.")
            return mapOf()
        }

        val channelId = prefs.getString("notif_${id}_channelId", "default") ?: "default"
        val title = prefs.getString("notif_${id}_title", "Reminder") ?: "Reminder"
        val text = prefs.getString("notif_${id}_text", "") ?: ""
        val hour = prefs.getInt("notif_${id}_hour", 13)
        val minute = prefs.getInt("notif_${id}_minute", 0)

        val daysStrings = prefs.getStringSet("notif_${id}_daysOfWeek", emptySet<String>()) ?: emptySet<String>()
        val days = daysStrings.map { it.toInt() }

        return mapOf(
            "id" to id,
            "channelId" to channelId,
            "title" to title,
            "text" to text,
            "hour" to hour,
            "minute" to minute,
            "daysOfWeek" to days
        )
    }


    // HELPER METHODS
    fun computeNextTriggerTime(daysOfWeek: List<Int>, hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()

        val candidate = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // search up to 7 days ahead
        for (i in 0..6) {
            val day = (now.get(java.util.Calendar.DAY_OF_WEEK) + i - 1) % 7 + 1
            if (daysOfWeek.contains(day)) {
                candidate.set(java.util.Calendar.DAY_OF_WEEK, day)
                if (candidate.timeInMillis > now.timeInMillis) {
                    return candidate.timeInMillis
                }
            }
            candidate.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        // fallback: one week from now
        return candidate.timeInMillis + 7 * 24 * 60 * 60 * 1000L
    }

    fun getPendingNotificationIntent(
        context : Context,
        id: Int
    ): PendingIntent {
        val appClass = Class.forName("com.godot.game.GodotApp")
        val appIntent = Intent(context, appClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            id,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    // NOTIFICATION PERMISSION
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


    // NOTIFICATION CHANNEL
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


    // SCHEDULE NOTIFICATION
    fun scheduleWeeklyNotification(
        context: Context?,
        id: Int,
        channelId: String,
        title: String,
        text: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>
    ) {
        if (daysOfWeek.isEmpty()) return
        val context = context ?:  return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = computeNextTriggerTime(daysOfWeek, hour, minute)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        saveNotification(
            context,
            id,
            channelId,
            title,
            text,
            hour,
            minute,
            daysOfWeek
        )

        Log.d(Constants.LOG_TAG, "Rolling notification scheduled for $triggerAtMillis ($daysOfWeek @ $hour:$minute)")
    }

    fun refreshScheduleWeeklyNotification(
        context: Context?,
        id: Int
    ) {
        val context = context ?:  return
        Log.d(Constants.LOG_TAG, "Refreshing scheduled notification.")

        val info = getNotification(context, id)
        if (info.isEmpty()) {
            Log.d(Constants.LOG_TAG, "No notification found.")
            return
        }

        val channelId = (info["channelId"] ?: "default") as String
        val title = (info["title"] ?: "Reminder") as String
        val text = (info["text"] ?: "") as String
        val hour = (info["hour"] ?: 13) as Int
        val minute = (info["minute"] ?: 0) as Int
        @Suppress("UNCHECKED_CAST")
        val days = (info["daysOfWeek"] ?: listOf<Int>()) as List<Int>

        Log.d(Constants.LOG_TAG, "Info: $info")

        scheduleWeeklyNotification(
            context,
            id,
            channelId,
            title,
            text,
            hour,
            minute,
            days
        )
    }

    fun scheduleInstantNotification(
        context : Context?,
        id: Int,
        channelId: String,
        title: String,
        text: String
    ) {
        val context = context ?: return

        val pendingNotificationIntent = getPendingNotificationIntent(context, id)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.btn_star)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(IMPORTANCE_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingNotificationIntent)

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