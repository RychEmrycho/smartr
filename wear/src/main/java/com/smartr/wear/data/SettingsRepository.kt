package com.smartr.wear.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "smartr_settings")

data class AppSettings(
    val sitThresholdMinutes: Int,
    val reminderRepeatMinutes: Int,
    val quietStartHour: Int,
    val quietEndHour: Int,
    val theme: ThemePreference
)

enum class ThemePreference {
    FOLLOW_SYSTEM, LIGHT, DARK
}

class SettingsRepository(private val context: Context) {
    private val syncManager = WearSyncManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val SIT_THRESHOLD_MINUTES = intPreferencesKey("sit_threshold_minutes")
        private val REPEAT_MINUTES = intPreferencesKey("repeat_minutes")
        private val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        private val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        private val THEME_PREFERENCE = intPreferencesKey("theme_preference")

        val DEFAULTS = AppSettings(
            sitThresholdMinutes = 45,
            reminderRepeatMinutes = 20,
            quietStartHour = 22,
            quietEndHour = 6,
            theme = ThemePreference.FOLLOW_SYSTEM
        )
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun ensureDefaults() {
        context.dataStore.edit { prefs ->
            if (!prefs.contains(SIT_THRESHOLD_MINUTES)) {
                prefs[SIT_THRESHOLD_MINUTES] = DEFAULTS.sitThresholdMinutes
            }
            if (!prefs.contains(REPEAT_MINUTES)) {
                prefs[REPEAT_MINUTES] = DEFAULTS.reminderRepeatMinutes
            }
            if (!prefs.contains(QUIET_START_HOUR)) {
                prefs[QUIET_START_HOUR] = DEFAULTS.quietStartHour
            }
            if (!prefs.contains(QUIET_END_HOUR)) {
                prefs[QUIET_END_HOUR] = DEFAULTS.quietEndHour
            }
            if (!prefs.contains(THEME_PREFERENCE)) {
                prefs[THEME_PREFERENCE] = DEFAULTS.theme.ordinal
            }
        }
    }

    suspend fun currentSettings(): AppSettings {
        ensureDefaults()
        return settings.first()
    }

    suspend fun updateSitThreshold(minutes: Int) {
        val newVal = minutes.coerceIn(15, 240)
        context.dataStore.edit { prefs ->
            prefs[SIT_THRESHOLD_MINUTES] = newVal
        }
        syncToWear()
    }

    suspend fun updateReminderRepeat(minutes: Int) {
        val newVal = minutes.coerceIn(5, 120)
        context.dataStore.edit { prefs ->
            prefs[REPEAT_MINUTES] = newVal
        }
        syncToWear()
    }

    suspend fun updateQuietStartHour(hour: Int) {
        val newVal = hour.coerceIn(0, 23)
        context.dataStore.edit { prefs ->
            prefs[QUIET_START_HOUR] = newVal
        }
        syncToWear()
    }

    suspend fun updateQuietEndHour(hour: Int) {
        val newVal = hour.coerceIn(0, 23)
        context.dataStore.edit { prefs ->
            prefs[QUIET_END_HOUR] = newVal
        }
        syncToWear()
    }

    suspend fun updateTheme(theme: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[THEME_PREFERENCE] = theme.ordinal
        }
    }

    private fun syncToWear() {
        scope.launch {
            syncManager.syncSettings(settings.first())
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        sitThresholdMinutes = this[SIT_THRESHOLD_MINUTES] ?: DEFAULTS.sitThresholdMinutes,
        reminderRepeatMinutes = this[REPEAT_MINUTES] ?: DEFAULTS.reminderRepeatMinutes,
        quietStartHour = this[QUIET_START_HOUR] ?: DEFAULTS.quietStartHour,
        quietEndHour = this[QUIET_END_HOUR] ?: DEFAULTS.quietEndHour,
        theme = ThemePreference.values()[this[THEME_PREFERENCE] ?: DEFAULTS.theme.ordinal]
    )
}
