package com.kisss.visualtime.completion

enum class AlarmScheduleResult {
    Exact,
    InexactFallback,
    Unavailable,
}

interface CompletionAlarmScheduler {
    fun schedule(token: String, triggerElapsedRealtimeMillis: Long): AlarmScheduleResult
    fun cancel(token: String?)
    fun claimForForeground(token: String): Boolean
    fun dismissNotification()

    companion object {
        val None = object : CompletionAlarmScheduler {
            override fun schedule(
                token: String,
                triggerElapsedRealtimeMillis: Long,
            ): AlarmScheduleResult = AlarmScheduleResult.Exact

            override fun cancel(token: String?) = Unit
            override fun claimForForeground(token: String): Boolean = true
            override fun dismissNotification() = Unit
        }
    }
}

fun interface CompletionTokenFactory {
    fun create(): String
}
