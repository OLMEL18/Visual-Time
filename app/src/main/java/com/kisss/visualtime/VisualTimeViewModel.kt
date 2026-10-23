package com.kisss.visualtime

import androidx.lifecycle.ViewModel
import com.kisss.visualtime.completion.AlarmScheduleResult
import com.kisss.visualtime.completion.CompletionAlarmScheduler
import com.kisss.visualtime.completion.CompletionTokenFactory
import com.kisss.visualtime.data.RecentDurationsPolicy
import com.kisss.visualtime.data.RecentDurationsStore
import com.kisss.visualtime.feedback.CompletionFeedback
import com.kisss.visualtime.timer.DurationFormatter
import com.kisss.visualtime.timer.DurationInputValidator
import com.kisss.visualtime.timer.MonotonicClock
import com.kisss.visualtime.timer.TimerEngine
import com.kisss.visualtime.timer.TimerSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class VisualTimeViewModel(
    private val clock: MonotonicClock,
    private val recentDurationsStore: RecentDurationsStore,
    private val completionFeedback: CompletionFeedback = CompletionFeedback.None,
    private val completionAlarmScheduler: CompletionAlarmScheduler = CompletionAlarmScheduler.None,
    private val completionTokenFactory: CompletionTokenFactory = CompletionTokenFactory {
        UUID.randomUUID().toString()
    },
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VisualTimeUiState())
    val uiState: StateFlow<VisualTimeUiState> = mutableUiState.asStateFlow()

    private var session: TimerSession? = null
    private var recentDurationsInitialized = false
    private var overlayHideDeadlineElapsedMillis: Long? = null
    private var completionAlarmToken: String? = null

    suspend fun initializeRecentDurations() {
        if (recentDurationsInitialized) return
        recentDurationsInitialized = true
        val durations = recentDurationsStore.loadOrSeed()
        mutableUiState.value = mutableUiState.value.copy(
            quickStartMinutes = durations,
            recentDurationsLoaded = true,
        )
    }

    fun onManualDraftChanged(value: String) {
        val existingError = mutableUiState.value.validationMessage
        mutableUiState.value = mutableUiState.value.copy(
            manualDraft = value,
            validationMessage = if (
                existingError != null && DurationInputValidator.parseValidMinutes(value) != null
            ) {
                null
            } else {
                existingError
            },
        )
    }

    suspend fun startQuickDuration(durationMinutes: Int) {
        require(durationMinutes > 0) { "Duration must be positive." }
        startDuration(durationMinutes)
    }

    suspend fun startManualDuration(): Boolean {
        val durationMinutes = DurationInputValidator.parseValidMinutes(
            mutableUiState.value.manualDraft,
        )
        if (durationMinutes == null) {
            mutableUiState.value = mutableUiState.value.copy(
                validationMessage = DurationInputValidator.ErrorMessage,
            )
            return false
        }
        startDuration(durationMinutes)
        return true
    }

    fun refreshFromClock() {
        if (mutableUiState.value.phase != TimerPhase.Running) return
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        publishSnapshot(currentSession, nowElapsedMillis = now)
        if (
            mutableUiState.value.phase == TimerPhase.Running &&
            mutableUiState.value.overlayVisible &&
            overlayHideDeadlineElapsedMillis?.let { now >= it } == true
        ) {
            dismissOverlay()
        }
    }

    fun onTimerSurfaceTapped() {
        if (mutableUiState.value.phase !in setOf(TimerPhase.Running, TimerPhase.Paused)) return
        if (mutableUiState.value.overlayVisible) {
            dismissOverlay()
        } else {
            revealOverlay()
        }
    }

    fun onSystemBack() {
        when (mutableUiState.value.phase) {
            TimerPhase.Running, TimerPhase.Paused -> {
                if (mutableUiState.value.overlayVisible) backToSetup() else revealOverlay()
            }
            TimerPhase.Finished -> backToSetup()
            TimerPhase.Setup -> Unit
        }
    }

    fun pause() {
        if (mutableUiState.value.phase != TimerPhase.Running) return
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        val pausedSession = TimerEngine.pause(currentSession, now)
        cancelCompletionAlarm()
        session = pausedSession
        overlayHideDeadlineElapsedMillis = null
        publishSnapshot(pausedSession, forcedPhase = TimerPhase.Paused, nowElapsedMillis = now)
        mutableUiState.value = mutableUiState.value.copy(overlayVisible = true)
    }

    fun resume() {
        if (mutableUiState.value.phase != TimerPhase.Paused) return
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        val resumedSession = TimerEngine.resume(currentSession, now)
        session = resumedSession
        publishSnapshot(resumedSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
        scheduleCompletionAlarm(resumedSession, now)
        mutableUiState.value = mutableUiState.value.copy(overlayVisible = true)
        overlayHideDeadlineElapsedMillis = now + OVERLAY_TIMEOUT_MILLIS
    }

    fun restart() {
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        cancelCompletionAlarm()
        val restartedSession = TimerEngine.restart(currentSession, now)
        session = restartedSession
        overlayHideDeadlineElapsedMillis = null
        publishSnapshot(restartedSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
        scheduleCompletionAlarm(restartedSession, now)
        mutableUiState.value = mutableUiState.value.copy(overlayVisible = false)
    }

    suspend fun repeat() {
        if (mutableUiState.value.phase != TimerPhase.Finished) return
        val durationMinutes = mutableUiState.value.selectedDurationMinutes ?: return
        startDuration(durationMinutes)
    }

    fun backToSetup() {
        cancelCompletionAlarm()
        session = null
        overlayHideDeadlineElapsedMillis = null
        mutableUiState.value = mutableUiState.value.copy(
            phase = TimerPhase.Setup,
            selectedDurationMinutes = null,
            elapsedProgress = 0f,
            formattedRemainingTime = null,
            overlayVisible = false,
        )
    }

    fun consumeBackgroundAlertWarning() {
        mutableUiState.value = mutableUiState.value.copy(backgroundAlertWarning = null)
    }

    private suspend fun startDuration(durationMinutes: Int) {
        val optimisticRecents = RecentDurationsPolicy.recordStarted(
            current = mutableUiState.value.quickStartMinutes,
            durationMinutes = durationMinutes,
        )
        val now = clock.elapsedRealtimeMillis()
        val newSession = TimerEngine.start(durationMinutes, now)
        cancelCompletionAlarm()
        session = newSession
        overlayHideDeadlineElapsedMillis = null
        mutableUiState.value = mutableUiState.value.copy(
            quickStartMinutes = optimisticRecents,
            recentDurationsLoaded = true,
            validationMessage = null,
            selectedDurationMinutes = durationMinutes,
            overlayVisible = false,
        )
        publishSnapshot(newSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
        scheduleCompletionAlarm(newSession, now)
        val persistedRecents = recentDurationsStore.recordStarted(durationMinutes)
        mutableUiState.value = mutableUiState.value.copy(quickStartMinutes = persistedRecents)
    }

    private fun publishSnapshot(
        currentSession: TimerSession,
        forcedPhase: TimerPhase? = null,
        nowElapsedMillis: Long = clock.elapsedRealtimeMillis(),
    ) {
        val previousPhase = mutableUiState.value.phase
        val snapshot = TimerEngine.snapshot(currentSession, nowElapsedMillis)
        val phase = when {
            snapshot.expired -> TimerPhase.Finished
            forcedPhase != null -> forcedPhase
            else -> TimerPhase.Running
        }
        if (snapshot.expired && currentSession.runningStartedAtElapsedMillis != null) {
            session = TimerEngine.pause(currentSession, nowElapsedMillis)
        }
        if (snapshot.expired) {
            overlayHideDeadlineElapsedMillis = null
        }
        mutableUiState.value = mutableUiState.value.copy(
            phase = phase,
            elapsedProgress = snapshot.elapsedProgress,
            formattedRemainingTime = DurationFormatter.format(
                remainingMillis = snapshot.remainingMillis,
                expired = snapshot.expired,
                format = currentSession.format,
            ),
            overlayVisible = if (snapshot.expired) false else mutableUiState.value.overlayVisible,
        )
        if (snapshot.expired && previousPhase != TimerPhase.Finished) {
            val token = completionAlarmToken
            val deliverForegroundFeedback = token == null ||
                completionAlarmScheduler.claimForForeground(token)
            completionAlarmToken = null
            completionAlarmScheduler.dismissNotification()
            if (deliverForegroundFeedback) completionFeedback.play()
        }
    }

    private fun scheduleCompletionAlarm(
        currentSession: TimerSession,
        nowElapsedMillis: Long,
    ) {
        val token = completionTokenFactory.create()
        completionAlarmToken = token
        val remainingMillis = TimerEngine.snapshot(currentSession, nowElapsedMillis).remainingMillis
        when (
            completionAlarmScheduler.schedule(
                token = token,
                triggerElapsedRealtimeMillis = nowElapsedMillis + remainingMillis,
            )
        ) {
            AlarmScheduleResult.Exact -> Unit
            AlarmScheduleResult.InexactFallback -> {
                mutableUiState.value = mutableUiState.value.copy(
                    backgroundAlertWarning = BackgroundAlertWarning.MayBeDelayed,
                )
            }
            AlarmScheduleResult.Unavailable -> {
                mutableUiState.value = mutableUiState.value.copy(
                    backgroundAlertWarning = BackgroundAlertWarning.Unavailable,
                )
            }
        }
    }

    private fun cancelCompletionAlarm() {
        completionAlarmScheduler.cancel(completionAlarmToken)
        completionAlarmToken = null
    }

    private fun revealOverlay() {
        val phase = mutableUiState.value.phase
        mutableUiState.value = mutableUiState.value.copy(overlayVisible = true)
        overlayHideDeadlineElapsedMillis = if (phase == TimerPhase.Running) {
            clock.elapsedRealtimeMillis() + OVERLAY_TIMEOUT_MILLIS
        } else {
            null
        }
    }

    private fun dismissOverlay() {
        overlayHideDeadlineElapsedMillis = null
        mutableUiState.value = mutableUiState.value.copy(overlayVisible = false)
    }

    private companion object {
        const val OVERLAY_TIMEOUT_MILLIS = 5_000L
    }
}
