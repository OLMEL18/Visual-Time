package com.kisss.visualtime

enum class TimerPhase {
    Setup,
    Running,
    Paused,
    Finished,
}

data class VisualTimeUiState(
    val phase: TimerPhase = TimerPhase.Setup,
    val quickStartMinutes: List<Int> = listOf(15, 25, 40),
    val selectedDurationMinutes: Int? = null,
    val elapsedProgress: Float = 0f,
    val formattedRemainingTime: String? = null,
)
