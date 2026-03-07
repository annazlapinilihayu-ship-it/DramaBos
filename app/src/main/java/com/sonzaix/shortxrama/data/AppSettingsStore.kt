package com.sonzaix.shortxrama.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.*

class AppSettingsStore(private val context: Context) {
    private val accentColorKey = stringPreferencesKey("accent_color")
    private val amoledKey = booleanPreferencesKey("amoled_mode")
    private val releaseNotifKey = booleanPreferencesKey("release_notifications")
    private val releaseScheduleKey = booleanPreferencesKey("release_schedule")
    private val keepAliveKey = booleanPreferencesKey("keep_alive_enabled")
    private val preferredQualityKey = intPreferencesKey("preferred_quality")
    private val hideWatchedKey = booleanPreferencesKey("hide_watched_dramas")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            accentColor = prefs[accentColorKey] ?: "#FF2965",
            amoledMode = prefs[amoledKey] ?: false,
            releaseNotifications = prefs[releaseNotifKey] ?: false,
            releaseSchedule = prefs[releaseScheduleKey] ?: false,
            keepAliveEnabled = prefs[keepAliveKey] ?: false,
            preferredQuality = prefs[preferredQualityKey] ?: 720,
            hideWatchedDramas = prefs[hideWatchedKey] ?: false
        )
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { prefs -> prefs[accentColorKey] = hex }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[amoledKey] = enabled }
    }

    suspend fun setReleaseNotifications(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[releaseNotifKey] = enabled }
    }

    suspend fun setReleaseSchedule(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[releaseScheduleKey] = enabled }
    }

    suspend fun setKeepAliveEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[keepAliveKey] = enabled }
    }

    suspend fun setPreferredQuality(value: Int) {
        context.dataStore.edit { prefs -> prefs[preferredQualityKey] = value }
    }

    suspend fun setHideWatchedDramas(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[hideWatchedKey] = enabled }
    }
}
