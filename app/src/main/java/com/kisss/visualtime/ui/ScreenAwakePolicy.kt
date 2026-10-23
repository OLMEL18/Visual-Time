package com.kisss.visualtime.ui

import com.kisss.visualtime.TimerPhase

internal object ScreenAwakePolicy {
    const val FinishedGraceMillis = 30_000L

    fun shouldKeepScreenOn(
        phase: TimerPhase,
        finishedGraceActive: Boolean,
    ): Boolean = when (phase) {
        TimerPhase.Running, TimerPhase.Paused -> true
        TimerPhase.Finished -> finishedGraceActive
        TimerPhase.Setup -> false
    }
}
