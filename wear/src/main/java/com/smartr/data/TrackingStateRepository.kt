package com.smartr.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartr.logic.InactivityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.trackingDataStore by preferencesDataStore(name = "tracking_state")

class TrackingStateRepository(private val context: Context) {
    companion object {
        private val SEDENTARY_START = longPreferencesKey("sedentary_start")
        private val LAST_MOVEMENT = longPreferencesKey("last_movement")
        private val LAST_REMINDER = longPreferencesKey("last_reminder")
        private val LAST_DAILY_STEPS = longPreferencesKey("last_daily_steps")
        private val SEDENTARY_BUFFER = longPreferencesKey("last_significant_movement")
        private val IS_OFF_BODY = booleanPreferencesKey("is_off_body")
    }

    val state: Flow<InactivityState> = context.trackingDataStore.data.map { prefs ->
        InactivityState(
            sedentaryStart = prefs[SEDENTARY_START]?.let { Instant.ofEpochMilli(it) },
            lastMovement = prefs[LAST_MOVEMENT]?.let { Instant.ofEpochMilli(it) },
            lastReminderAt = prefs[LAST_REMINDER]?.let { Instant.ofEpochMilli(it) },
            lastSignificantMovementAt = prefs[SEDENTARY_BUFFER]?.let { Instant.ofEpochMilli(it) }
        )
    }

    val lastDailySteps: Flow<Long?> = context.trackingDataStore.data.map { it[LAST_DAILY_STEPS] }
    val isOffBody: Flow<Boolean> = context.trackingDataStore.data.map { it[IS_OFF_BODY] ?: false }

    suspend fun updateState(state: InactivityState) {
        context.trackingDataStore.edit { prefs ->
            state.sedentaryStart?.toEpochMilli()?.let { prefs[SEDENTARY_START] = it } ?: prefs.remove(SEDENTARY_START)
            state.lastMovement?.toEpochMilli()?.let { prefs[LAST_MOVEMENT] = it } ?: prefs.remove(LAST_MOVEMENT)
            state.lastReminderAt?.toEpochMilli()?.let { prefs[LAST_REMINDER] = it } ?: prefs.remove(LAST_REMINDER)
            state.lastSignificantMovementAt?.toEpochMilli()?.let { prefs[SEDENTARY_BUFFER] = it } ?: prefs.remove(SEDENTARY_BUFFER)
        }
    }

    suspend fun updateSteps(steps: Long) {
        context.trackingDataStore.edit { it[LAST_DAILY_STEPS] = steps }
    }

    suspend fun setOffBody(offBody: Boolean) {
        context.trackingDataStore.edit { it[IS_OFF_BODY] = offBody }
    }
    
    suspend fun reset() {
        context.trackingDataStore.edit { 
            it.remove(SEDENTARY_START)
            it.remove(LAST_REMINDER)
        }
    }
}
