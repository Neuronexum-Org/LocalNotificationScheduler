package org.godotengine.plugin.android.localnotificationscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class BootReceiver : BroadcastReceiver() {
    private val notificationHandler = NotificationHandler()


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(Constants.LOG_TAG, "Device rebooted, restoring scheduled notifications...")

            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val ids : Set<String> = prefs.getStringSet(Constants.NOTIF_ID_NAME,  setOf<String>())!!

            for (idStr in ids) {
                val id = idStr.toInt()

                notificationHandler.refreshScheduleWeeklyNotification(context, id)
            }
        }
    }
}
