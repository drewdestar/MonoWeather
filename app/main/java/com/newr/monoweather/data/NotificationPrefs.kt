/*
 * =====================================================
 *  MONOWEATHER
 *  Weather App by NEWR Labs
 *  Copyright (c) 2026 NEWR Labs. All rights reserved.
 * =====================================================
 * 
 *  Connect with NEWR Labs:
 *  - X (Twitter)  : @NEWROPLY
 *  - Instagram    : @NEWROPLY
 *  - TikTok       : @NEWROPLY
 *  - Threads      : @NEWROPLY
 *  - Discord      : https://discord.gg/vZU3KzEh6a
 *  - GitHub       : https://github.com/NEWR-LABS
 * 
 *  "Just the weather. Nothing else."
 * =====================================================
 */
package com.newr.monoweather.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class NotificationPrefs(private val context: Context) {
    companion object {
        val RAIN_ALERT = booleanPreferencesKey("rain_alert")
        val TEMP_ALERT = booleanPreferencesKey("temp_alert")
        val DAILY_SUMMARY = booleanPreferencesKey("daily_summary")
        val HOURLY_UPDATE = booleanPreferencesKey("hourly_update")
    }

    val rainAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[RAIN_ALERT] ?: true
    }

    val tempAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TEMP_ALERT] ?: true
    }

    val dailySummaryEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DAILY_SUMMARY] ?: true
    }

    val hourlyUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HOURLY_UPDATE] ?: false
    }

    suspend fun setRainAlert(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[RAIN_ALERT] = enabled
        }
    }

    suspend fun setTempAlert(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TEMP_ALERT] = enabled
        }
    }

    suspend fun setDailySummary(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DAILY_SUMMARY] = enabled
        }
    }

    suspend fun setHourlyUpdate(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HOURLY_UPDATE] = enabled
        }
    }
}
