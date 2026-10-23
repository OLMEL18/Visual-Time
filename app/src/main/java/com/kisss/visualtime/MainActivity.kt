package com.kisss.visualtime

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kisss.visualtime.completion.AndroidCompletionAlarmScheduler
import com.kisss.visualtime.completion.AppVisibilityTracker
import com.kisss.visualtime.completion.CompletionNotification
import com.kisss.visualtime.data.RecentDurationsRepository
import com.kisss.visualtime.data.visualTimeDataStore
import com.kisss.visualtime.feedback.AndroidCompletionFeedback
import com.kisss.visualtime.timer.AndroidMonotonicClock
import com.kisss.visualtime.ui.VisualTimeScreen
import com.kisss.visualtime.ui.theme.VisualTimeTheme

class MainActivity : ComponentActivity() {
    private var notificationWarningShownThisProcess = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) showNotificationUnavailableWarning()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val recentDurationsRepository = RecentDurationsRepository(applicationContext.visualTimeDataStore)
        val completionFeedback = AndroidCompletionFeedback(applicationContext)
        val completionAlarmScheduler = AndroidCompletionAlarmScheduler(applicationContext)
        if (AppVisibilityTracker.isFirstActivityCreationInProcess()) {
            // A newly created process has no restorable timer session in this POC.
            // Cancel any durable orphan so a normal setup relaunch cannot alert later.
            completionAlarmScheduler.cancel(token = null)
        }
        CompletionNotification(applicationContext).ensureChannel()

        setContent {
            VisualTimeTheme {
                val viewModel: VisualTimeViewModel = viewModel(
                    factory = VisualTimeViewModelFactory(
                        clock = AndroidMonotonicClock,
                        recentDurationsStore = recentDurationsRepository,
                        completionFeedback = completionFeedback,
                        completionAlarmScheduler = completionAlarmScheduler,
                    ),
                )
                VisualTimeScreen(
                    viewModel = viewModel,
                    onTimerStarted = ::requestNotificationPermissionIfNeeded,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibilityTracker.onActivityStarted()
    }

    override fun onStop() {
        AppVisibilityTracker.onActivityStopped()
        super.onStop()
    }

    private fun requestNotificationPermissionIfNeeded() {
        requestExactAlarmAccessIfNeeded()
        val completionNotification = CompletionNotification(applicationContext)
        completionNotification.ensureChannel()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (!completionNotification.canPost()) showNotificationUnavailableWarning()
            return
        }
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val preferences = getSharedPreferences(PermissionPreferencesName, MODE_PRIVATE)
        if (!preferences.getBoolean(NotificationPermissionRequestedKey, false)) {
            preferences.edit().putBoolean(NotificationPermissionRequestedKey, true).apply()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotificationUnavailableWarning()
        }
    }

    private fun requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT !in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2) return
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return

        val preferences = getSharedPreferences(PermissionPreferencesName, MODE_PRIVATE)
        if (preferences.getBoolean(ExactAlarmAccessRequestedKey, false)) return
        preferences.edit().putBoolean(ExactAlarmAccessRequestedKey, true).apply()
        Toast.makeText(
            this,
            getString(R.string.exact_alarm_access_explanation),
            Toast.LENGTH_LONG,
        ).show()
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }

    private fun showNotificationUnavailableWarning() {
        if (notificationWarningShownThisProcess) return
        notificationWarningShownThisProcess = true
        Toast.makeText(
            this,
            getString(R.string.background_notifications_unavailable),
            Toast.LENGTH_LONG,
        ).show()
    }

    private companion object {
        const val PermissionPreferencesName = "visual_time_permission_prompts"
        const val NotificationPermissionRequestedKey = "notification_permission_requested"
        const val ExactAlarmAccessRequestedKey = "exact_alarm_access_requested"
    }
}
