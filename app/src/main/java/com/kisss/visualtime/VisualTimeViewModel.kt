package com.kisss.visualtime

import androidx.lifecycle.ViewModel
import com.kisss.visualtime.timer.DurationFormatter
import com.kisss.visualtime.timer.MonotonicClock
import com.kisss.visualtime.timer.TimerEngine
import com.kisss.visualtime.timer.TimerSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisualTimeViewModel(
    private val clock: MonotonicClock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(VisualTimeUiState())
    val uiState: StateFlow<VisualTimeUiState> = mutableUiState.asStateFlow()

    private var session: TimerSession? = null

    fun startQuickDuration(durationMinutes: Int) {
        require(durationMinutes > 0) { "Duration must be positive." }
        startSession(durationMinutes, updateRecent(durationMinutes))
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

    fun repeat() {
        if (mutableUiState.value.phase != TimerPhase.Finished) return
        val durationMinutes = mutableUiState.value.selectedDurationMinutes ?: return
        startSession(durationMinutes, updateRecent(durationMinutes))
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

    private fun startSession(durationMinutes: Int, recents: List<Int>) {
        val now = clock.elapsedRealtimeMillis()
        val newSession = TimerEngine.start(durationMinutes, now)
        session = newSession
        mutableUiState.value = mutableUiState.value.copy(
            quickStartMinutes = recents,
            selectedDurationMinutes = durationMinutes,
        )
        publishSnapshot(newSession, forcedPhase = TimerPhase.Running, nowElapsedMillis = now)
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

    private fun updateRecent(durationMinutes: Int): List<Int> =
        (listOf(durationMinutes) + mutableUiState.value.quickStartMinutes)
            .distinct()
            .take(3)
}
