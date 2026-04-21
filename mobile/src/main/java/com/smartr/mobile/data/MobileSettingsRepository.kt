package com.smartr.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mobile_settings")

data class WatchSettings(
    val thresholdValue: Int,
    val thresholdUnit: TimeIntervalUnit,
    val repeatValue: Int,
    val repeatUnit: TimeIntervalUnit,
    val quietStartHour: Int,
    val quietEndHour: Int
)

class MobileSettingsRepository(private val context: Context) {
    companion object {
        private val THRESHOLD_VALUE = intPreferencesKey("threshold_value")
        private val THRESHOLD_UNIT = intPreferencesKey("threshold_unit")
        private val REPEAT_VALUE = intPreferencesKey("repeat_value")
        private val REPEAT_UNIT = intPreferencesKey("repeat_unit")
        private val QUIET_START = intPreferencesKey("quiet_start")
        private val QUIET_END = intPreferencesKey("quiet_end")

        val DEFAULTS = WatchSettings(45, TimeIntervalUnit.MINUTES, 20, TimeIntervalUnit.MINUTES, 22, 6)
    }

    val watchSettings: Flow<WatchSettings> = context.dataStore.data.map { prefs ->
        WatchSettings(
            thresholdValue = prefs[THRESHOLD_VALUE] ?: DEFAULTS.thresholdValue,
            thresholdUnit = TimeIntervalUnit.entries[prefs[THRESHOLD_UNIT] ?: DEFAULTS.thresholdUnit.ordinal],
            repeatValue = prefs[REPEAT_VALUE] ?: DEFAULTS.repeatValue,
            repeatUnit = TimeIntervalUnit.entries[prefs[REPEAT_UNIT] ?: DEFAULTS.repeatUnit.ordinal],
            quietStartHour = prefs[QUIET_START] ?: DEFAULTS.quietStartHour,
            quietEndHour = prefs[QUIET_END] ?: DEFAULTS.quietEndHour
        )
    }

    suspend fun updateFromWatch(
        thresholdValue: Int,
        thresholdUnit: Int,
        repeatValue: Int,
        repeatUnit: Int,
        quietStart: Int,
        quietEnd: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[THRESHOLD_VALUE] = thresholdValue
            prefs[THRESHOLD_UNIT] = thresholdUnit
            prefs[REPEAT_VALUE] = repeatValue
            prefs[REPEAT_UNIT] = repeatUnit
            prefs[QUIET_START] = quietStart
            prefs[QUIET_END] = quietEnd
        }
    }
}
