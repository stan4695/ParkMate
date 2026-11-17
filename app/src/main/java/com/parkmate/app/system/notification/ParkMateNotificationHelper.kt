package com.parkmate.app.system.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.parkmate.app.MainActivity
import com.parkmate.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkMateNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_TIMER = "parkmate_timer_alerts"
        const val CHANNEL_AUTOSAVE = "parkmate_autosave_alerts"
        const val NOTIF_ID_WARNING = 2001
        const val NOTIF_ID_EXPIRED = 2002
        const val NOTIF_ID_AUTOSAVE = 2003
    }

    fun ensureChannelsCreated() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.notif_channel_timer_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_timer_desc)
            enableVibration(true)
        }

        val autoSaveChannel = NotificationChannel(
            CHANNEL_AUTOSAVE,
            context.getString(R.string.notif_channel_autosave_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_autosave_desc)
        }

        manager.createNotificationChannels(listOf(timerChannel, autoSaveChannel))
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("MissingPermission")
    fun showTimerWarningNotification(addressLine: String) {
        if (!hasNotificationPermission()) return
        ensureChannelsCreated()

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_car)
            .setContentTitle(context.getString(R.string.notif_timer_warning_title))
            .setContentText(context.getString(R.string.notif_timer_warning_body, addressLine))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createAppPendingIntent())
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_WARNING, notification)
        }
    }

    @Suppress("MissingPermission")
    fun showTimerExpiredNotification(addressLine: String) {
        if (!hasNotificationPermission()) return
        ensureChannelsCreated()

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_car)
            .setContentTitle(context.getString(R.string.notif_timer_expired_title))
            .setContentText(context.getString(R.string.notif_timer_expired_body, addressLine))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createAppPendingIntent())
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_EXPIRED, notification)
        }
    }

    @Suppress("MissingPermission")
    fun showAutoSavedNotification(addressLine: String) {
        if (!hasNotificationPermission()) return
        ensureChannelsCreated()

        val notification = NotificationCompat.Builder(context, CHANNEL_AUTOSAVE)
            .setSmallIcon(R.drawable.ic_launcher_car)
            .setContentTitle(context.getString(R.string.notif_autosave_title))
            .setContentText(context.getString(R.string.notif_autosave_body, addressLine))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createAppPendingIntent())
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_AUTOSAVE, notification)
        }
    }
}
