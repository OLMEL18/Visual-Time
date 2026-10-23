package com.kisss.visualtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kisss.visualtime.completion.CompletionAlarmScheduler
import com.kisss.visualtime.data.RecentDurationsStore
import com.kisss.visualtime.feedback.CompletionFeedback
import com.kisss.visualtime.timer.MonotonicClock

class VisualTimeViewModelFactory(
    private val clock: MonotonicClock,
    private val recentDurationsStore: RecentDurationsStore,
    private val completionFeedback: CompletionFeedback,
    private val completionAlarmScheduler: CompletionAlarmScheduler,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(VisualTimeViewModel::class.java))
        return VisualTimeViewModel(
            clock = clock,
            recentDurationsStore = recentDurationsStore,
            completionFeedback = completionFeedback,
            completionAlarmScheduler = completionAlarmScheduler,
        ) as T
    }
}
