package org.godotengine.plugin.android.localnotificationscheduler

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

private const val REQUEST_NOTIF_PERMISSION = 1001

@Suppress("unused")
class GodotAndroidPlugin(godot: Godot) : GodotPlugin(godot) {

    val notificationHandler = NotificationHandler()

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(
            SignalInfo("permission_granted"),
            SignalInfo("permission_denied")
        )
    }

    override fun onMainRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>?,
        grantResults: IntArray?
    ) {
        super.onMainRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIF_PERMISSION) {
            if (grantResults != null && grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                emitSignal("permission_granted")
                Log.d(Constants.LOG_TAG, "Notification permission granted by user.")
            } else {
                if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val prefs = activity!!.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit {
                        putBoolean(
                            Constants.NOTFI_PERFS_NAME,
                            !activity!!.shouldShowRequestPermissionRationale(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                        apply()
                    }
                }

                emitSignal("permission_denied")
                Log.d(Constants.LOG_TAG, "Notification permission denied by user.")
            }
        }
    }

    private fun putString(p0: String, p1: Boolean) {}


    @UsedByGodot
    fun requestNotificationPermission() {
        notificationHandler.requestNotificationPermission(activity)
    }

    @UsedByGodot
    fun hasNotificationPermission(): Boolean {
        return notificationHandler.hasNotificationPermission(activity)
    }

    @UsedByGodot
    fun isNotificationPermissionPermanentlyDenied(): Boolean {
        val activity = activity ?: return false

        val prefs = activity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.NOTFI_PERFS_NAME, false)
    }


    @UsedByGodot
    fun createNotificationChannel() {
        notificationHandler.createNotificationChannel(
            activity,
            Constants.CHANNEL_ID
        )
    }

    @UsedByGodot
    fun scheduleWeeklyNotification(
        id: Int,
        title: String,
        text: String,
        hour: Int,
        minute: Int,
        daysOfWeek: IntArray
    ) {
        notificationHandler.scheduleWeeklyNotification(
            activity,
            id,
            Constants.CHANNEL_ID,
            title,
            text,
            hour,
            minute,
            daysOfWeek.toList()
        )
    }

    @UsedByGodot
    fun refreshScheduleWeeklyNotification(id: Int) {
        notificationHandler.refreshScheduleWeeklyNotification(
            activity,
            id
        )
    }

    @UsedByGodot
    fun scheduleInstantNotification(
        id: Int,
        title: String,
        text: String
    ) {
        notificationHandler.scheduleInstantNotification(
            activity,
            id,
            Constants.CHANNEL_ID,
            title,
            text
        )
    }

    @UsedByGodot
    fun cancelScheduledNotification(id: Int) {
        notificationHandler.cancelScheduledNotification(activity, id)
    }


    @UsedByGodot
    fun openAppSettings() {
        activity?.let {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", it.packageName, null)
            it.startActivity(intent)
        }
    }

    @UsedByGodot
    fun logMessage(message: String) {
        Log.d(Constants.LOG_TAG, message)
    }
}
