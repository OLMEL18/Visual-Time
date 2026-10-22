package com.kisss.visualtime

import com.kisss.visualtime.timer.MonotonicClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisualTimeViewModelTest {
    @Test
    fun quickStartRunsToFinishedThenRepeatAndBackUseTheSameSessionDuration() {
        val clock = FakeMonotonicClock()
        val viewModel = VisualTimeViewModel(clock)

        viewModel.startQuickDuration(15)
        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertEquals(15, viewModel.uiState.value.selectedDurationMinutes)
        assertEquals(listOf(15, 25, 40), viewModel.uiState.value.quickStartMinutes)
        assertEquals("15:00", viewModel.uiState.value.formattedRemainingTime)

        clock.nowMillis = 450_000L
        viewModel.refreshFromClock()
        assertEquals(0.5f, viewModel.uiState.value.elapsedProgress, 0.0001f)

        clock.nowMillis = 900_000L
        viewModel.refreshFromClock()
        assertEquals(TimerPhase.Finished, viewModel.uiState.value.phase)
        assertEquals(1f, viewModel.uiState.value.elapsedProgress, 0.0001f)
        assertEquals("00:00", viewModel.uiState.value.formattedRemainingTime)

        viewModel.repeat()
        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertEquals(0f, viewModel.uiState.value.elapsedProgress, 0.0001f)
        assertEquals("15:00", viewModel.uiState.value.formattedRemainingTime)

        viewModel.backToSetup()
        assertEquals(TimerPhase.Setup, viewModel.uiState.value.phase)
        assertEquals(0f, viewModel.uiState.value.elapsedProgress, 0.0001f)
        assertNull(viewModel.uiState.value.selectedDurationMinutes)
    }

    @Test
    fun pauseResumeAndRestartDoNotLoseOrInventElapsedTime() {
        val clock = FakeMonotonicClock()
        val viewModel = VisualTimeViewModel(clock)
        viewModel.startQuickDuration(25)

        clock.nowMillis = 100_000L
        viewModel.pause()
        val pausedProgress = viewModel.uiState.value.elapsedProgress
        assertEquals(TimerPhase.Paused, viewModel.uiState.value.phase)

        clock.nowMillis = 500_000L
        viewModel.refreshFromClock()
        assertEquals(pausedProgress, viewModel.uiState.value.elapsedProgress, 0.0001f)

        viewModel.resume()
        clock.nowMillis = 600_000L
        viewModel.refreshFromClock()
        assertEquals(200_000f / 1_500_000f, viewModel.uiState.value.elapsedProgress, 0.0001f)

        viewModel.restart()
        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertEquals(0f, viewModel.uiState.value.elapsedProgress, 0.0001f)
        assertEquals("25:00", viewModel.uiState.value.formattedRemainingTime)
    }

    @Test
    fun longSessionKeepsLongFormatAtCompletion() {
        val clock = FakeMonotonicClock()
        val viewModel = VisualTimeViewModel(clock)
        viewModel.startQuickDuration(90)
        assertEquals("01:30:00", viewModel.uiState.value.formattedRemainingTime)

        clock.nowMillis = 5_400_000L
        viewModel.refreshFromClock()
        assertEquals(TimerPhase.Finished, viewModel.uiState.value.phase)
        assertEquals("00:00:00", viewModel.uiState.value.formattedRemainingTime)
    }

    private class FakeMonotonicClock(
        var nowMillis: Long = 0L,
    ) : MonotonicClock {
        override fun elapsedRealtimeMillis(): Long = nowMillis
    }
}
