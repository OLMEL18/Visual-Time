package com.kisss.visualtime

import androidx.lifecycle.ViewModel
import com.kisss.visualtime.data.RecentDurationsPolicy
import com.kisss.visualtime.data.RecentDurationsStore
import com.kisss.visualtime.timer.DurationFormatter
import com.kisss.visualtime.timer.DurationInputValidator
import com.kisss.visualtime.timer.MonotonicClock
import com.kisss.visualtime.timer.TimerEngine
import com.kisss.visualtime.timer.TimerSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisualTimeViewModel(
    private val clock: MonotonicClock,
    private val recentDurationsStore: RecentDurationsStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VisualTimeUiState())
    val uiState: StateFlow<VisualTimeUiState> = mutableUiState.asStateFlow()

    private var session: TimerSession? = null
    private var recentDurationsInitialized = false

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
        publishSnapshot(currentSession)
    }

    fun pause() {
        if (mutableUiState.value.phase != TimerPhase.Running) return
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        val pausedSession = TimerEngine.pause(currentSession, now)
        session = pausedSession
        publishSnapshot(pausedSession, forcedPhase = TimerPhase.Paused, nowElapsedMillis = now)
    }

    fun resume() {
        if (mutableUiState.value.phase != TimerPhase.Paused) return
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        val resumedSession = TimerEngine.resume(currentSession, now)
        session = resumedSession
        publishSnapshot(resumedSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
    }

    fun restart() {
        val currentSession = session ?: return
        val now = clock.elapsedRealtimeMillis()
        val restartedSession = TimerEngine.restart(currentSession, now)
        session = restartedSession
        publishSnapshot(restartedSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
    }

    suspend fun repeat() {
        if (mutableUiState.value.phase != TimerPhase.Finished) return
        val durationMinutes = mutableUiState.value.selectedDurationMinutes ?: return
        startDuration(durationMinutes)
    }

    fun backToSetup() {
        session = null
        mutableUiState.value = mutableUiState.value.copy(
            phase = TimerPhase.Setup,
            selectedDurationMinutes = null,
            elapsedProgress = 0f,
            formattedRemainingTime = null,
        )
    }

    private suspend fun startDuration(durationMinutes: Int) {
        val optimisticRecents = RecentDurationsPolicy.recordStarted(
            current = mutableUiState.value.quickStartMinutes,
            durationMinutes = durationMinutes,
        )
        val now = clock.elapsedRealtimeMillis()
        val newSession = TimerEngine.start(durationMinutes, now)
        session = newSession
        mutableUiState.value = mutableUiState.value.copy(
            quickStartMinutes = optimisticRecents,
            recentDurationsLoaded = true,
            validationMessage = null,
            selectedDurationMinutes = durationMinutes,
        )
        publishSnapshot(newSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
        val persistedRecents = recentDurationsStore.recordStarted(durationMinutes)
        mutableUiState.value = mutableUiState.value.copy(quickStartMinutes = persistedRecents)
    }

    private fun publishSnapshot(
        currentSession: TimerSession,
        forcedPhase: TimerPhase? = null,
        nowElapsedMillis: Long = clock.elapsedRealtimeMillis(),
    ) {
        val snapshot = TimerEngine.snapshot(currentSession, nowElapsedMillis)
        val phase = when {
            snapshot.expired -> TimerPhase.Finished
            forcedPhase != null -> forcedPhase
            else -> TimerPhase.Running
        }
        if (snapshot.expired && currentSession.runningStartedAtElapsedMillis != null) {
            session = TimerEngine.pause(currentSession, nowElapsedMillis)
        }
        mutableUiState.value = mutableUiState.value.copy(
            phase = phase,
            elapsedProgress = snapshot.elapsedProgress,
            formattedRemainingTime = DurationFormatter.format(
                remainingMillis = snapshot.remainingMillis,
                expired = snapshot.expired,
                format = currentSession.format,
            ),
        )
    }
}
