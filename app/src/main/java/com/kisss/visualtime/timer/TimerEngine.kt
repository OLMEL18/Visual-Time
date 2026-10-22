package com.kisss.visualtime.timer

import kotlin.math.max

object TimerEngine {
    private const val MILLIS_PER_MINUTE = 60_000L

    fun start(durationMinutes: Int, nowElapsedMillis: Long): TimerSession {
        require(durationMinutes > 0) { "Duration must be positive." }
        val durationMillis = durationMinutes.toLong() * MILLIS_PER_MINUTE
        return TimerSession(
            originalDurationMillis = durationMillis,
            accumulatedElapsedMillis = 0L,
            runningStartedAtElapsedMillis = nowElapsedMillis,
            format = if (durationMinutes < 60) DurationFormat.Short else DurationFormat.Long,
        )
    }

    fun snapshot(session: TimerSession, nowElapsedMillis: Long): TimerSnapshot {
        val runningElapsed = session.runningStartedAtElapsedMillis?.let { anchor ->
            max(0L, nowElapsedMillis - anchor)
        } ?: 0L
        val elapsed = (session.accumulatedElapsedMillis + runningElapsed)
            .coerceIn(0L, session.originalDurationMillis)
        val remaining = session.originalDurationMillis - elapsed
        return TimerSnapshot(
            remainingMillis = remaining,
            elapsedProgress = if (session.originalDurationMillis == 0L) {
                1f
            } else {
                (elapsed.toDouble() / session.originalDurationMillis).toFloat().coerceIn(0f, 1f)
            },
            expired = remaining == 0L,
        )
    }

    fun pause(session: TimerSession, nowElapsedMillis: Long): TimerSession {
        if (session.runningStartedAtElapsedMillis == null) return session
        val snapshot = snapshot(session, nowElapsedMillis)
        return session.copy(
            accumulatedElapsedMillis = session.originalDurationMillis - snapshot.remainingMillis,
            runningStartedAtElapsedMillis = null,
        )
    }

    fun resume(session: TimerSession, nowElapsedMillis: Long): TimerSession {
        if (session.runningStartedAtElapsedMillis != null) return session
        if (session.accumulatedElapsedMillis >= session.originalDurationMillis) return session
        return session.copy(runningStartedAtElapsedMillis = nowElapsedMillis)
    }

    fun restart(session: TimerSession, nowElapsedMillis: Long): TimerSession =
        session.copy(
            accumulatedElapsedMillis = 0L,
            runningStartedAtElapsedMillis = nowElapsedMillis,
        )
}
