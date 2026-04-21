package com.smartr.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mobile_settings")

data class WatchSettings(
    val sitThresholdMinutes: Int,
    val reminderRepeatMinutes: Int,
    val quietStartHour: Int,
    val quietEndHour: Int
)

class MobileSettingsRepository(private val context: Context) {
    companion object {
        private val SIT_THRESHOLD = intPreferencesKey("sit_threshold")
        private val REPEAT_MINUTES = intPreferencesKey("repeat_minutes")
        private val QUIET_START = intPreferencesKey("quiet_start")
        private val QUIET_END = intPreferencesKey("quiet_end")

        val DEFAULTS = WatchSettings(45, 20, 22, 6)
    }

    val watchSettings: Flow<WatchSettings> = context.dataStore.data.map { prefs ->
        WatchSettings(
            sitThresholdMinutes = prefs[SIT_THRESHOLD] ?: DEFAULTS.sitThresholdMinutes,
            reminderRepeatMinutes = prefs[REPEAT_MINUTES] ?: DEFAULTS.reminderRepeatMinutes,
            quietStartHour = prefs[QUIET_START] ?: DEFAULTS.quietStartHour,
            quietEndHour = prefs[QUIET_END] ?: DEFAULTS.quietEndHour
        )
    }

    suspend fun updateFromWatch(
        sitThreshold: Int,
        repeatMinutes: Int,
        quietStart: Int,
        quietEnd: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[SIT_THRESHOLD] = sitThreshold
            prefs[REPEAT_MINUTES] = repeatMinutes
            prefs[QUIET_START] = quietStart
            prefs[QUIET_END] = quietEnd
        }
    }
}
