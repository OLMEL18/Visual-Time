package com.kisss.visualtime.ui

import com.kisss.visualtime.TimerPhase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAwakePolicyTest {
    @Test
    fun runningAndPausedKeepScreenOn() {
        assertTrue(ScreenAwakePolicy.shouldKeepScreenOn(TimerPhase.Running, false))
        assertTrue(ScreenAwakePolicy.shouldKeepScreenOn(TimerPhase.Paused, false))
    }

    @Test
    fun setupUsesNormalScreenTimeout() {
        assertFalse(ScreenAwakePolicy.shouldKeepScreenOn(TimerPhase.Setup, true))
    }

    @Test
    fun finishedKeepsScreenOnOnlyDuringGracePeriod() {
        assertTrue(ScreenAwakePolicy.shouldKeepScreenOn(TimerPhase.Finished, true))
        assertFalse(ScreenAwakePolicy.shouldKeepScreenOn(TimerPhase.Finished, false))
    }
}
