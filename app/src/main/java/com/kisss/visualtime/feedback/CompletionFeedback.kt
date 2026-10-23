package com.kisss.visualtime.feedback

fun interface CompletionFeedback {
    fun play()

    companion object {
        val None = CompletionFeedback {}
    }
}
