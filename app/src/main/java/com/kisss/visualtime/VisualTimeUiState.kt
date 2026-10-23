package com.kisss.visualtime

enum class TimerPhase {
    Setup,
    Running,
    Paused,
    Finished,
}

enum class BackgroundAlertWarning {
    MayBeDelayed,
    Unavailable,
}

data class VisualTimeUiState(
    val phase: TimerPhase = TimerPhase.Setup,
    val quickStartMinutes: List<Int> = emptyList(),
    val recentDurationsLoaded: Boolean = false,
    val manualDraft: String = "",
    val validationMessage: String? = null,
    val selectedDurationMinutes: Int? = null,
    val elapsedProgress: Float = 0f,
    val formattedRemainingTime: String? = null,
    val overlayVisible: Boolean = false,
    val backgroundAlertWarning: BackgroundAlertWarning? = null,
)
