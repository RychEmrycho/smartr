package com.smartr.logic

import com.smartr.data.AppSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class InactivityState(
    val sedentaryStart: Instant?,
    val lastMovement: Instant?,
    val lastReminderAt: Instant?,
    val lastSignificantMovementAt: Instant? = null
)

data class InactivityDecision(
    val shouldRemind: Boolean,
    val reason: String
)

class InactivityEngine(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun evaluate(
        state: InactivityState,
        now: Instant,
        settings: AppSettings,
        movementDetected: Boolean,
        isWatchSleeping: Boolean = false,
        isOffBody: Boolean = false,
        isCharging: Boolean = false,
        isDndEnabled: Boolean = false,
        isExercising: Boolean = false
    ): Pair<InactivityState, InactivityDecision> {
        if (isOffBody) {
            return state.copy(sedentaryStart = null) to InactivityDecision(false, "watch_off_body")
        }

        if (isCharging) {
            return state.copy(sedentaryStart = null) to InactivityDecision(false, "watch_charging")
        }

        if (isExercising) {
            return state.copy(sedentaryStart = null, lastSignificantMovementAt = now) to InactivityDecision(false, "user_exercising")
        }

        if (movementDetected) {
            val movementStart = state.lastSignificantMovementAt ?: now
            val movementDuration = Duration.between(movementStart, now)
            
            val bufferThreshold = settings.movementBufferUnit.toDuration(settings.movementBufferValue)

            return if (movementDuration < bufferThreshold) {
                // Short move: keep sedentaryStart, update lastMovement and track start of this move
                state.copy(
                    lastMovement = now,
                    lastSignificantMovementAt = movementStart
                ) to InactivityDecision(false, "short_movement_detected")
            } else {
                // Sustained move: reset sedentaryStart
                state.copy(
                    sedentaryStart = null,
                    lastMovement = now,
                    lastSignificantMovementAt = now,
                    lastReminderAt = null
                ) to InactivityDecision(false, "movement_reset")
            }
        }

        // Reset movement start time when no movement is detected
        val stateWithoutMovement = if (state.lastSignificantMovementAt != null) {
            state.copy(lastSignificantMovementAt = null)
        } else {
            state
        }

        if (settings.isSleeping || isWatchSleeping) {
            return stateWithoutMovement to InactivityDecision(false, "user_sleeping")
        }

        if (isDndEnabled) {
            return stateWithoutMovement to InactivityDecision(false, "dnd_enabled")
        }

        if (isQuietHours(now, settings)) {
            val quietState = if (stateWithoutMovement.sedentaryStart == null) stateWithoutMovement.copy(sedentaryStart = now) else stateWithoutMovement
            return quietState to InactivityDecision(false, "quiet_hours")
        }

        val updatedState = if (stateWithoutMovement.sedentaryStart == null) stateWithoutMovement.copy(sedentaryStart = now) else stateWithoutMovement
        val sedentaryStart = updatedState.sedentaryStart ?: now
        val sedentaryDuration = Duration.between(sedentaryStart, now)
        val sitThreshold = settings.sitThresholdUnit.toDuration(settings.sitThresholdValue)
        
        if (sedentaryDuration < sitThreshold) {
            return updatedState to InactivityDecision(false, "below_threshold")
        }

        val canRepeat = updatedState.lastReminderAt?.let {
            val repeatDuration = Duration.between(it, now)
            val repeatThreshold = settings.reminderRepeatUnit.toDuration(settings.reminderRepeatValue)
            repeatDuration >= repeatThreshold
        } ?: true

        return if (canRepeat) {
            updatedState.copy(lastReminderAt = now) to InactivityDecision(true, "threshold_reached")
        } else {
            updatedState to InactivityDecision(false, "repeat_window")
        }
    }

    private fun isQuietHours(now: Instant, settings: AppSettings): Boolean {
        val localTime = now.atZone(zoneId).toLocalTime()
        val start = LocalTime.of(settings.quietStartHour, 0)
        val end = LocalTime.of(settings.quietEndHour, 0)
        return if (start <= end) {
            localTime >= start && localTime < end
        } else {
            localTime >= start || localTime < end
        }
    }
}
