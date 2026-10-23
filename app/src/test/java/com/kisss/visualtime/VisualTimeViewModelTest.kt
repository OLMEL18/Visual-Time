package com.kisss.visualtime

import com.kisss.visualtime.completion.AlarmScheduleResult
import com.kisss.visualtime.completion.CompletionAlarmScheduler
import com.kisss.visualtime.completion.CompletionTokenFactory
import com.kisss.visualtime.timer.MonotonicClock
import com.kisss.visualtime.data.RecentDurationsPolicy
import com.kisss.visualtime.data.RecentDurationsStore
import com.kisss.visualtime.feedback.CompletionFeedback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun runningOverlayRevealsAndAutoHidesAtFiveSeconds() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(15)

        assertFalse(viewModel.uiState.value.overlayVisible)
        viewModel.onTimerSurfaceTapped()
        assertTrue(viewModel.uiState.value.overlayVisible)

        clock.nowMillis = 4_999L
        viewModel.refreshFromClock()
        assertTrue(viewModel.uiState.value.overlayVisible)

        clock.nowMillis = 5_000L
        viewModel.refreshFromClock()
        assertFalse(viewModel.uiState.value.overlayVisible)
    }

    @Test
    fun pausedOverlayDoesNotTimeoutButCanBeDismissedAndRevealed() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(25)
        viewModel.onTimerSurfaceTapped()

        clock.nowMillis = 1_000L
        viewModel.pause()
        assertEquals(TimerPhase.Paused, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.overlayVisible)

        clock.nowMillis = 60_000L
        viewModel.refreshFromClock()
        assertTrue(viewModel.uiState.value.overlayVisible)

        viewModel.onTimerSurfaceTapped()
        assertFalse(viewModel.uiState.value.overlayVisible)
        viewModel.onTimerSurfaceTapped()
        assertTrue(viewModel.uiState.value.overlayVisible)

        viewModel.resume()
        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.overlayVisible)
        clock.nowMillis = 65_000L
        viewModel.refreshFromClock()
        assertFalse(viewModel.uiState.value.overlayVisible)
    }

    @Test
    fun restartReturnsToCleanRunningViewAndSystemBackUsesTwoSteps() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(15)

        clock.nowMillis = 100_000L
        viewModel.onSystemBack()
        assertTrue(viewModel.uiState.value.overlayVisible)

        viewModel.restart()
        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertEquals(0f, viewModel.uiState.value.elapsedProgress, 0.0001f)
        assertFalse(viewModel.uiState.value.overlayVisible)

        viewModel.onSystemBack()
        assertTrue(viewModel.uiState.value.overlayVisible)
        viewModel.onSystemBack()
        assertEquals(TimerPhase.Setup, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.overlayVisible)
    }

    @Test
    fun finishedSystemBackReturnsToSetup() = runBlocking {
        val clock = FakeMonotonicClock()
        val viewModel = createViewModel(clock)
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(1)
        clock.nowMillis = 60_000L
        viewModel.refreshFromClock()

        assertEquals(TimerPhase.Finished, viewModel.uiState.value.phase)
        viewModel.onSystemBack()
        assertEquals(TimerPhase.Setup, viewModel.uiState.value.phase)
    }

    @Test
    fun completionFeedbackPlaysExactlyOncePerFinishedSession() = runBlocking {
        val clock = FakeMonotonicClock()
        val feedback = FakeCompletionFeedback()
        val viewModel = createViewModel(clock, feedback)
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(1)

        viewModel.pause()
        viewModel.resume()
        viewModel.restart()
        assertEquals(0, feedback.playCount)

        clock.nowMillis = 60_000L
        viewModel.refreshFromClock()
        viewModel.refreshFromClock()
        assertEquals(1, feedback.playCount)

        viewModel.repeat()
        clock.nowMillis = 120_000L
        viewModel.refreshFromClock()
        assertEquals(2, feedback.playCount)
    }

    @Test
    fun completionAlarmsFollowStartPauseResumeRestartAndBack() = runBlocking {
        val clock = FakeMonotonicClock()
        val scheduler = FakeCompletionAlarmScheduler()
        val tokens = ArrayDeque(listOf("start", "resume", "restart"))
        val viewModel = createViewModel(
            clock = clock,
            completionAlarmScheduler = scheduler,
            completionTokenFactory = CompletionTokenFactory { tokens.removeFirst() },
        )
        viewModel.initializeRecentDurations()

        viewModel.startQuickDuration(1)
        assertEquals(ScheduledAlarm("start", 60_000L), scheduler.scheduled.last())

        clock.nowMillis = 10_000L
        viewModel.pause()
        assertEquals("start", scheduler.canceled.last())

        clock.nowMillis = 20_000L
        viewModel.resume()
        assertEquals(ScheduledAlarm("resume", 70_000L), scheduler.scheduled.last())

        clock.nowMillis = 30_000L
        viewModel.restart()
        assertEquals("resume", scheduler.canceled.last())
        assertEquals(ScheduledAlarm("restart", 90_000L), scheduler.scheduled.last())

        viewModel.backToSetup()
        assertEquals("restart", scheduler.canceled.last())
    }

    @Test
    fun consumedBackgroundClaimPreventsDuplicateForegroundFeedback() = runBlocking {
        val clock = FakeMonotonicClock()
        val feedback = FakeCompletionFeedback()
        val scheduler = FakeCompletionAlarmScheduler(claimForeground = false)
        val viewModel = createViewModel(
            clock = clock,
            completionFeedback = feedback,
            completionAlarmScheduler = scheduler,
        )
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(1)

        clock.nowMillis = 60_000L
        viewModel.refreshFromClock()

        assertEquals(TimerPhase.Finished, viewModel.uiState.value.phase)
        assertEquals(0, feedback.playCount)
        assertEquals(1, scheduler.dismissCount)
    }

    @Test
    fun unavailableExactAlarmProducesConsumableWarningWithoutBlockingTimer() = runBlocking {
        val scheduler = FakeCompletionAlarmScheduler(
            scheduleResult = AlarmScheduleResult.InexactFallback,
        )
        val viewModel = createViewModel(
            clock = FakeMonotonicClock(),
            completionAlarmScheduler = scheduler,
        )
        viewModel.initializeRecentDurations()
        viewModel.startQuickDuration(1)

        assertEquals(TimerPhase.Running, viewModel.uiState.value.phase)
        assertEquals(
            BackgroundAlertWarning.MayBeDelayed,
            viewModel.uiState.value.backgroundAlertWarning,
        )
        viewModel.consumeBackgroundAlertWarning()
        assertNull(viewModel.uiState.value.backgroundAlertWarning)
    }

    private fun createViewModel(
        clock: FakeMonotonicClock,
        completionFeedback: CompletionFeedback = CompletionFeedback.None,
        completionAlarmScheduler: CompletionAlarmScheduler = CompletionAlarmScheduler.None,
        completionTokenFactory: CompletionTokenFactory = CompletionTokenFactory { "test-token" },
    ): VisualTimeViewModel = VisualTimeViewModel(
        clock = clock,
        recentDurationsStore = FakeRecentDurationsStore(),
        completionFeedback = completionFeedback,
        completionAlarmScheduler = completionAlarmScheduler,
        completionTokenFactory = completionTokenFactory,
    )

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

    private class FakeCompletionFeedback : CompletionFeedback {
        var playCount = 0

        override fun play() {
            playCount += 1
        }
    }

    private data class ScheduledAlarm(
        val token: String,
        val triggerElapsedRealtimeMillis: Long,
    )

    private class FakeCompletionAlarmScheduler(
        private val scheduleResult: AlarmScheduleResult = AlarmScheduleResult.Exact,
        private val claimForeground: Boolean = true,
    ) : CompletionAlarmScheduler {
        val scheduled = mutableListOf<ScheduledAlarm>()
        val canceled = mutableListOf<String?>()
        var dismissCount = 0

        override fun schedule(
            token: String,
            triggerElapsedRealtimeMillis: Long,
        ): AlarmScheduleResult {
            scheduled += ScheduledAlarm(token, triggerElapsedRealtimeMillis)
            return scheduleResult
        }

        override fun cancel(token: String?) {
            canceled += token
        }

        override fun claimForForeground(token: String): Boolean = claimForeground

        override fun dismissNotification() {
            dismissCount += 1
        }
    }
}
