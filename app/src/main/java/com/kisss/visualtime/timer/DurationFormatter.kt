package com.kisss.visualtime.timer

object DurationFormatter {
    fun format(
        remainingMillis: Long,
        expired: Boolean,
        format: DurationFormat,
    ): String {
        val displayedSeconds = if (expired || remainingMillis <= 0L) {
            0L
        } else {
            (remainingMillis + 999L) / 1_000L
        }
        val seconds = displayedSeconds % 60L
        val totalMinutes = displayedSeconds / 60L
        return when (format) {
            DurationFormat.Short -> "%02d:%02d".format(totalMinutes, seconds)
            DurationFormat.Long -> {
                val hours = totalMinutes / 60L
                val minutes = totalMinutes % 60L
                "%02d:%02d:%02d".format(hours, minutes, seconds)
            }
        }
    }
}
