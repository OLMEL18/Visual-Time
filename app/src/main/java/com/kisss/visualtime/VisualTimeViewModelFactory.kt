package com.kisss.visualtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kisss.visualtime.timer.MonotonicClock

class VisualTimeViewModelFactory(
    private val clock: MonotonicClock,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(VisualTimeViewModel::class.java))
        return VisualTimeViewModel(clock) as T
    }
}
