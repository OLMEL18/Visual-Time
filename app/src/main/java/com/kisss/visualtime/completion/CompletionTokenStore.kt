package com.kisss.visualtime.completion

import android.content.Context

interface CompletionTokenStore {
    fun activate(token: String)
    fun clear(token: String?)
    fun isActive(token: String): Boolean
    fun consumeIfActive(token: String): Boolean
}

class SharedPreferencesCompletionTokenStore(context: Context) : CompletionTokenStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )

    override fun activate(token: String) = synchronized(Lock) {
        preferences.edit().putString(ActiveTokenKey, token).commit()
        Unit
    }

    override fun clear(token: String?) = synchronized(Lock) {
        val activeToken = preferences.getString(ActiveTokenKey, null)
        if (token == null || activeToken == token) {
            preferences.edit().remove(ActiveTokenKey).commit()
        }
    }

    override fun isActive(token: String): Boolean = synchronized(Lock) {
        preferences.getString(ActiveTokenKey, null) == token
    }

    override fun consumeIfActive(token: String): Boolean = synchronized(Lock) {
        if (preferences.getString(ActiveTokenKey, null) != token) {
            return@synchronized false
        }
        preferences.edit().remove(ActiveTokenKey).commit()
    }

    private companion object {
        const val PreferencesName = "visual_time_completion_alarm"
        const val ActiveTokenKey = "active_session_token"
        val Lock = Any()
    }
}

class CompletionDeliveryCoordinator(
    private val tokenStore: CompletionTokenStore,
) {
    fun claimForForeground(token: String): Boolean = tokenStore.consumeIfActive(token)

    fun claimForBackground(
        token: String,
        appVisible: Boolean,
        notificationsAvailable: Boolean,
    ): Boolean {
        if (appVisible || !notificationsAvailable) return false
        return tokenStore.consumeIfActive(token)
    }
}
