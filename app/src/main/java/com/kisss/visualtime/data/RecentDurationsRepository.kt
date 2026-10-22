package com.kisss.visualtime.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

interface RecentDurationsStore {
    suspend fun loadOrSeed(): List<Int>
    suspend fun recordStarted(durationMinutes: Int): List<Int>
}

class RecentDurationsRepository(
    private val dataStore: DataStore<Preferences>,
) : RecentDurationsStore {
    override suspend fun loadOrSeed(): List<Int> {
        val preferences = dataStore.data.first()
        val initialization = RecentDurationsPolicy.initialize(
            preferenceExists = preferences.contains(RecentDurationsKey),
            encodedValue = preferences[RecentDurationsKey],
        )
        initialization.encodedValueToSeed?.let { encodedDefaults ->
            dataStore.edit { mutablePreferences ->
                if (!mutablePreferences.contains(RecentDurationsKey)) {
                    mutablePreferences[RecentDurationsKey] = encodedDefaults
                }
            }
        }
        return initialization.durations
    }

    override suspend fun recordStarted(durationMinutes: Int): List<Int> {
        var updatedDurations = emptyList<Int>()
        dataStore.edit { mutablePreferences ->
            val initialization = RecentDurationsPolicy.initialize(
                preferenceExists = mutablePreferences.contains(RecentDurationsKey),
                encodedValue = mutablePreferences[RecentDurationsKey],
            )
            updatedDurations = RecentDurationsPolicy.recordStarted(
                current = initialization.durations,
                durationMinutes = durationMinutes,
            )
            mutablePreferences[RecentDurationsKey] = RecentDurationsPolicy.encode(updatedDurations)
        }
        return updatedDurations
    }

    private companion object {
        val RecentDurationsKey = stringPreferencesKey("recent_durations_ordered")
    }
}

val Context.visualTimeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "visual_time_preferences",
)
