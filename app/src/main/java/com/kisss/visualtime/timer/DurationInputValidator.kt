package com.kisss.visualtime.timer

object DurationInputValidator {
    const val ErrorMessage = "Enter 1–1440 minutes"

    fun parseValidMinutes(value: String): Int? {
        if (value.isEmpty() || value.any { !it.isDigit() }) return null
        val parsed = value.toLongOrNull() ?: return null
        return parsed.takeIf { it in 1L..1_440L }?.toInt()
    }
}
