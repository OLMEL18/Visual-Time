package com.kisss.visualtime.completion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CompletionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidCompletionAlarmScheduler.ActionCompletionAlarm) return
        val token = intent.getStringExtra(AndroidCompletionAlarmScheduler.ExtraSessionToken)
            ?: return
        val applicationContext = context.applicationContext
        val tokenStore = SharedPreferencesCompletionTokenStore(applicationContext)
        val deliveryCoordinator = CompletionDeliveryCoordinator(tokenStore)
        val notification = CompletionNotification(applicationContext)

        if (!tokenStore.isActive(token)) {
            Log.i(LogTag, "Ignored stale completion alarm")
            return
        }
        if (AppVisibilityTracker.isVisible) {
            Log.i(LogTag, "Visible app will deliver foreground completion feedback")
            return
        }
        if (!notification.canPost()) {
            Log.w(LogTag, "Completion notification unavailable; preserving foreground claim")
            return
        }
        if (
            deliveryCoordinator.claimForBackground(
                token = token,
                appVisible = AppVisibilityTracker.isVisible,
                notificationsAvailable = true,
            )
        ) {
            if (notification.show()) {
                Log.i(LogTag, "Background completion notification posted")
            } else {
                Log.e(LogTag, "Completion notification could not be posted")
            }
        }
    }

    private companion object {
        const val LogTag = "VisualTimeAlarm"
    }
}
