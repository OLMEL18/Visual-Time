package com.kisss.visualtime.timer

enum class DurationFormat {
    Short,
    Long,
}

data class TimerSession(
    val originalDurationMillis: Long,
    val accumulatedElapsedMillis: Long,
    val runningStartedAtElapsedMillis: Long?,
    val format: DurationFormat,
)

data class TimerSnapshot(
    val remainingMillis: Long,
    val elapsedProgress: Float,
    val expired: Boolean,
)
