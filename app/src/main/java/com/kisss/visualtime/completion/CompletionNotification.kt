package com.kisss.visualtime.completion

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.kisss.visualtime.MainActivity
import com.kisss.visualtime.R

class CompletionNotification(context: Context) {
    private val applicationContext = context.applicationContext
    private val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            ChannelId,
            applicationContext.getString(R.string.timer_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = applicationContext.getString(R.string.timer_notification_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 90L, 90L, 70L)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun canPost(): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!notificationManager.areNotificationsEnabled()) return false
        return notificationManager.getNotificationChannel(ChannelId)?.importance !=
            NotificationManager.IMPORTANCE_NONE
    }

    fun show(): Boolean = runCatching {
        ensureChannel()
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            ContentRequestCode,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(applicationContext, ChannelId)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(R.string.time_is_up))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(NotificationId, notification)
    }.isSuccess

    fun dismiss() {
        notificationManager.cancel(NotificationId)
    }

    companion object {
        const val ChannelId = "visual_time_completion_v1"
        const val NotificationId = 1_001
        private const val ContentRequestCode = 2_002
    }
}
