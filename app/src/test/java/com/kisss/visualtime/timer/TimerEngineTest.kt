package com.kisss.visualtime.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
    @Test
    fun startAndMidpointUseMonotonicElapsedTime() {
        val session = TimerEngine.start(durationMinutes = 10, nowElapsedMillis = 1_000L)

        val start = TimerEngine.snapshot(session, 1_000L)
        val midpoint = TimerEngine.snapshot(session, 301_000L)

        assertEquals(600_000L, start.remainingMillis)
        assertEquals(0f, start.elapsedProgress, 0.0001f)
        assertFalse(start.expired)
        assertEquals(300_000L, midpoint.remainingMillis)
        assertEquals(0.5f, midpoint.elapsedProgress, 0.0001f)
        assertFalse(midpoint.expired)
    }

    @Test
    fun exactAndOverExpiryClampToTheFinishedSnapshot() {
        val session = TimerEngine.start(durationMinutes = 1, nowElapsedMillis = 5_000L)

        val exact = TimerEngine.snapshot(session, 65_000L)
        val over = TimerEngine.snapshot(session, 95_000L)

        listOf(exact, over).forEach { snapshot ->
            assertEquals(0L, snapshot.remainingMillis)
            assertEquals(1f, snapshot.elapsedProgress, 0.0001f)
            assertTrue(snapshot.expired)
        }
    }

    @Test
    fun pauseResumeAndRestartPreserveTheCorrectElapsedAmount() {
        val initial = TimerEngine.start(durationMinutes = 5, nowElapsedMillis = 1_000L)
        val paused = TimerEngine.pause(initial, nowElapsedMillis = 61_000L)

        assertEquals(60_000L, paused.accumulatedElapsedMillis)
        assertNull(paused.runningStartedAtElapsedMillis)
        assertEquals(240_000L, TimerEngine.snapshot(paused, 161_000L).remainingMillis)

        val resumed = TimerEngine.resume(paused, nowElapsedMillis = 161_000L)
        assertEquals(180_000L, TimerEngine.snapshot(resumed, 221_000L).remainingMillis)

        val restarted = TimerEngine.restart(resumed, nowElapsedMillis = 300_000L)
        assertEquals(300_000L, TimerEngine.snapshot(restarted, 300_000L).remainingMillis)
        assertEquals(0f, TimerEngine.snapshot(restarted, 300_000L).elapsedProgress, 0.0001f)
    }
}
