package com.kisss.visualtime

import com.kisss.visualtime.timer.MonotonicClock
import com.kisss.visualtime.data.RecentDurationsPolicy
import com.kisss.visualtime.data.RecentDurationsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisualTimeViewModelTest {
    @Test
    fun quickStartRunsToFinishedThenRepeatAndBackUseTheSameSessionDuration() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()

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
    fun pauseResumeAndRestartDoNotLoseOrInventElapsedTime() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()
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
    fun longSessionKeepsLongFormatAtCompletion() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(90)
        assertEquals("01:30:00", viewModel.uiState.value.formattedRemainingTime)

        clock.nowMillis = 5_400_000L
        viewModel.refreshFromClock()
        assertEquals(TimerPhase.Finished, viewModel.uiState.value.phase)
        assertEquals("00:00:00", viewModel.uiState.value.formattedRemainingTime)
    }

    @Test
    fun manualDraftIsSeparateFromQuickStartsAndInvalidInputDoesNotStart() = runBlocking {
        val viewModel = createViewModel(FakeMonotonicClock())
        viewModel.initializeRecentDurations()

        viewModel.onManualDraftChanged("30")
        viewModel.startQuickDuration(25)
        viewModel.backToSetup()
        assertEquals("30", viewModel.uiState.value.manualDraft)

        viewModel.onManualDraftChanged("999999999999999999999999")
        assertEquals(false, viewModel.startManualDuration())
        assertEquals(TimerPhase.Setup, viewModel.uiState.value.phase)
        assertEquals("Enter 1–1440 minutes", viewModel.uiState.value.validationMessage)

        viewModel.onManualDraftChanged("40")
        assertNull(viewModel.uiState.value.validationMessage)
        assertEquals(true, viewModel.startManualDuration())
        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertEquals(listOf(40, 25, 15), viewModel.uiState.value.quickStartMinutes)
    }

    private fun createViewModel(clock: FakeMonotonicClock): VisualTimeViewModel =
        VisualTimeViewModel(clock, FakeRecentDurationsStore())

    private class FakeMonotonicClock(
        var nowMillis: Long = 0L,
    ) : MonotonicClock {
        override fun elapsedRealtimeMillis(): Long = nowMillis
    }

    private class FakeRecentDurationsStore(
        initial: List<Int> = RecentDurationsPolicy.Defaults,
    ) : RecentDurationsStore {
        private var durations = initial

        override suspend fun loadOrSeed(): List<Int> = durations

        override suspend fun recordStarted(durationMinutes: Int): List<Int> {
            durations = RecentDurationsPolicy.recordStarted(durations, durationMinutes)
            return durations
        }
    }
}
