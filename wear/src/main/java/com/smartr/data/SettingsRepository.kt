package com.smartr.data

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

enum class TimeIntervalUnit {
    SECONDS, MINUTES, HOURS;

    fun toDuration(value: Int): java.time.Duration = when (this) {
        SECONDS -> java.time.Duration.ofSeconds(value.toLong())
        MINUTES -> java.time.Duration.ofMinutes(value.toLong())
        HOURS -> java.time.Duration.ofHours(value.toLong())
    }
}

data class AppSettings(
    val sitThresholdValue: Int,
    val sitThresholdUnit: TimeIntervalUnit,
    val reminderRepeatValue: Int,
    val reminderRepeatUnit: TimeIntervalUnit,
    val quietStartHour: Int,
    val quietEndHour: Int,
    val theme: ThemePreference,
    val isSleeping: Boolean = false
)

enum class ThemePreference {
    AUTO, LIGHT, DARK
}

class SettingsRepository(private val context: Context) {
    private val syncManager = WearSyncManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val SIT_THRESHOLD_VALUE = intPreferencesKey("sit_threshold_value")
        private val SIT_THRESHOLD_UNIT = intPreferencesKey("sit_threshold_unit")
        private val REPEAT_VALUE = intPreferencesKey("repeat_value")
        private val REPEAT_UNIT = intPreferencesKey("repeat_unit")
        
        // Legacy keys for migration
        private val SIT_THRESHOLD_MINUTES_LEGACY = intPreferencesKey("sit_threshold_minutes")
        private val REPEAT_MINUTES_LEGACY = intPreferencesKey("repeat_minutes")
        
        private val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        private val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        private val THEME_PREFERENCE = intPreferencesKey("theme_preference")
        private val IS_SLEEPING = androidx.datastore.preferences.core.booleanPreferencesKey("is_sleeping")

        val DEFAULTS = AppSettings(
            sitThresholdValue = 45,
            sitThresholdUnit = TimeIntervalUnit.MINUTES,
            reminderRepeatValue = 20,
            reminderRepeatUnit = TimeIntervalUnit.MINUTES,
            quietStartHour = 22,
            quietEndHour = 6,
            theme = ThemePreference.AUTO,
            isSleeping = false
        )
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun ensureDefaults() {
        context.dataStore.edit { prefs ->
            if (!prefs.contains(SIT_THRESHOLD_VALUE) && !prefs.contains(SIT_THRESHOLD_MINUTES_LEGACY)) {
                prefs[SIT_THRESHOLD_VALUE] = DEFAULTS.sitThresholdValue
                prefs[SIT_THRESHOLD_UNIT] = DEFAULTS.sitThresholdUnit.ordinal
            }
            if (!prefs.contains(REPEAT_VALUE) && !prefs.contains(REPEAT_MINUTES_LEGACY)) {
                prefs[REPEAT_VALUE] = DEFAULTS.reminderRepeatValue
                prefs[REPEAT_UNIT] = DEFAULTS.reminderRepeatUnit.ordinal
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

    suspend fun updateSitThresholdValue(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[SIT_THRESHOLD_VALUE] = value
        }
        syncToWear()
    }

    suspend fun updateSitThresholdUnit(unit: TimeIntervalUnit) {
        context.dataStore.edit { prefs ->
            prefs[SIT_THRESHOLD_UNIT] = unit.ordinal
        }
        syncToWear()
    }

    suspend fun updateReminderRepeatValue(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[REPEAT_VALUE] = value
        }
        syncToWear()
    }

    suspend fun updateReminderRepeatUnit(unit: TimeIntervalUnit) {
        context.dataStore.edit { prefs ->
            prefs[REPEAT_UNIT] = unit.ordinal
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

    suspend fun updateSleepStatus(isSleeping: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_SLEEPING] = isSleeping
        }
    }

    private fun syncToWear() {
        scope.launch {
            syncManager.syncSettings(settings.first())
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        // Migration logic: if legacy exists but new doesn't, use legacy and default unit to MINUTES
        val sitValue = this[SIT_THRESHOLD_VALUE] ?: this[SIT_THRESHOLD_MINUTES_LEGACY] ?: DEFAULTS.sitThresholdValue
        val sitUnit = this[SIT_THRESHOLD_UNIT]?.let { TimeIntervalUnit.entries[it] } ?: TimeIntervalUnit.MINUTES
        
        val repeatValue = this[REPEAT_VALUE] ?: this[REPEAT_MINUTES_LEGACY] ?: DEFAULTS.reminderRepeatValue
        val repeatUnit = this[REPEAT_UNIT]?.let { TimeIntervalUnit.entries[it] } ?: TimeIntervalUnit.MINUTES

        return AppSettings(
            sitThresholdValue = sitValue,
            sitThresholdUnit = sitUnit,
            reminderRepeatValue = repeatValue,
            reminderRepeatUnit = repeatUnit,
            quietStartHour = this[QUIET_START_HOUR] ?: DEFAULTS.quietStartHour,
            quietEndHour = this[QUIET_END_HOUR] ?: DEFAULTS.quietEndHour,
            theme = ThemePreference.entries[this[THEME_PREFERENCE] ?: DEFAULTS.theme.ordinal],
            isSleeping = this[IS_SLEEPING] ?: DEFAULTS.isSleeping
        )
    }
}
