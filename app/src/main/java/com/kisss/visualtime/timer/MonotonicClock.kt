package com.kisss.visualtime.timer

import android.os.SystemClock

fun interface MonotonicClock {
    fun elapsedRealtimeMillis(): Long
}

object AndroidMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
