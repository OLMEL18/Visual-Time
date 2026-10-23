package com.kisss.visualtime.completion

import java.util.concurrent.atomic.AtomicBoolean

object AppVisibilityTracker {
    private val activityCreatedInThisProcess = AtomicBoolean(false)

    @Volatile
    var isVisible: Boolean = false
        private set

    fun onActivityStarted() {
        isVisible = true
    }

    fun onActivityStopped() {
        isVisible = false
    }

    fun isFirstActivityCreationInProcess(): Boolean =
        activityCreatedInThisProcess.compareAndSet(false, true)
}
