package com.kisss.visualtime.completion

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AndroidCompletionAlarmScheduler(context: Context) : CompletionAlarmScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)
    private val tokenStore = SharedPreferencesCompletionTokenStore(applicationContext)
    private val deliveryCoordinator = CompletionDeliveryCoordinator(tokenStore)
    private val notification = CompletionNotification(applicationContext)

    override fun schedule(
        token: String,
        triggerElapsedRealtimeMillis: Long,
    ): AlarmScheduleResult {
        cancelPendingIntentOnly()
        notification.dismiss()
        tokenStore.activate(token)
        val pendingIntent = completionPendingIntent(token)

        return try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerElapsedRealtimeMillis,
                    pendingIntent,
                )
                Log.i(LogTag, "Scheduled exact completion alarm")
                AlarmScheduleResult.Exact
            } else {
                scheduleInexact(triggerElapsedRealtimeMillis, pendingIntent)
            }
        } catch (securityException: SecurityException) {
            Log.w(LogTag, "Exact alarm unavailable; using inexact fallback", securityException)
            scheduleInexact(triggerElapsedRealtimeMillis, pendingIntent)
        } catch (error: RuntimeException) {
            Log.e(LogTag, "Unable to schedule completion alarm", error)
            AlarmScheduleResult.Unavailable
        }
    }

    override fun cancel(token: String?) {
        cancelPendingIntentOnly()
        tokenStore.clear(token)
        notification.dismiss()
    }

    override fun claimForForeground(token: String): Boolean {
        val claimed = deliveryCoordinator.claimForForeground(token)
        if (claimed) cancelPendingIntentOnly()
        return claimed
    }

    override fun dismissNotification() {
        notification.dismiss()
    }

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun scheduleInexact(
        triggerElapsedRealtimeMillis: Long,
        pendingIntent: PendingIntent,
    ): AlarmScheduleResult = try {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerElapsedRealtimeMillis,
            pendingIntent,
        )
        Log.w(LogTag, "Scheduled inexact completion fallback")
        AlarmScheduleResult.InexactFallback
    } catch (error: RuntimeException) {
        Log.e(LogTag, "Unable to schedule inexact completion fallback", error)
        AlarmScheduleResult.Unavailable
    }

    private fun cancelPendingIntentOnly() {
        alarmManager.cancel(completionPendingIntent(token = null))
    }

    private fun completionPendingIntent(token: String?): PendingIntent {
        val intent = Intent(applicationContext, CompletionAlarmReceiver::class.java).apply {
            action = ActionCompletionAlarm
            token?.let { putExtra(ExtraSessionToken, it) }
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            AlarmRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ActionCompletionAlarm = "com.kisss.visualtime.action.COMPLETION_ALARM"
        const val ExtraSessionToken = "session_token"
        private const val AlarmRequestCode = 1_001
        private const val LogTag = "VisualTimeAlarm"
    }
}
