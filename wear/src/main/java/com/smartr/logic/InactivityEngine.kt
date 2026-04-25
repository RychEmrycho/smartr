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
        val bufferThreshold = settings.movementBufferUnit.toDuration(settings.movementBufferValue)

        // 1. Check for conditions that suppress tracking/reminders
        checkSuppression(state, now, isOffBody, isCharging, isExercising)?.let { return it }

        // 2. Process movement if detected
        if (movementDetected) {
            return processMovement(state, now, bufferThreshold)
        }

        // 3. Handle grace period for movement start time
        val updatedState = handleMovementGracePeriod(state, now, bufferThreshold)

        // 4. Check for conditions that suppress reminders (but keep tracking)
        checkReminderSuppression(updatedState, now, settings, isWatchSleeping, isDndEnabled)?.let { return it }

        // 5. Evaluate sedentary duration
        return evaluateSedentaryThreshold(updatedState, now, settings)
    }

    private fun checkSuppression(
        state: InactivityState,
        now: Instant,
        isOffBody: Boolean,
        isCharging: Boolean,
        isExercising: Boolean
    ): Pair<InactivityState, InactivityDecision>? {
        return when {
            isOffBody -> state.copy(sedentaryStart = null) to InactivityDecision(false, "watch_off_body")
            isCharging -> state.copy(sedentaryStart = null) to InactivityDecision(false, "watch_charging")
            isExercising -> state.copy(sedentaryStart = null, lastSignificantMovementAt = now) to InactivityDecision(false, "user_exercising")
            else -> null
        }
    }

    private fun processMovement(
        state: InactivityState,
        now: Instant,
        bufferThreshold: Duration
    ): Pair<InactivityState, InactivityDecision> {
        val movementStart = state.lastSignificantMovementAt ?: now
        val movementDuration = Duration.between(movementStart, now)

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

    private fun handleMovementGracePeriod(
        state: InactivityState,
        now: Instant,
        bufferThreshold: Duration
    ): InactivityState {
        val timeSinceLastMove = state.lastMovement?.let { Duration.between(it, now) } ?: Duration.ZERO
        return if (state.lastSignificantMovementAt != null && timeSinceLastMove > bufferThreshold) {
            state.copy(lastSignificantMovementAt = null)
        } else {
            state
        }
    }

    private fun checkReminderSuppression(
        state: InactivityState,
        now: Instant,
        settings: AppSettings,
        isWatchSleeping: Boolean,
        isDndEnabled: Boolean
    ): Pair<InactivityState, InactivityDecision>? {
        return when {
            settings.isSleeping || isWatchSleeping -> state to InactivityDecision(false, "user_sleeping")
            isDndEnabled -> state to InactivityDecision(false, "dnd_enabled")
            isQuietHours(now, settings) -> {
                val quietState = state.sedentaryStart?.let { state } ?: state.copy(sedentaryStart = now)
                quietState to InactivityDecision(false, "quiet_hours")
            }
            else -> null
        }
    }

    private fun evaluateSedentaryThreshold(
        state: InactivityState,
        now: Instant,
        settings: AppSettings
    ): Pair<InactivityState, InactivityDecision> {
        val updatedState = state.sedentaryStart?.let { state } ?: state.copy(sedentaryStart = now)
        val sedentaryStart = updatedState.sedentaryStart ?: now
        val sedentaryDuration = Duration.between(sedentaryStart, now)
        val sitThreshold = settings.sitThresholdUnit.toDuration(settings.sitThresholdValue)

        if (sedentaryDuration < sitThreshold) {
            return updatedState to InactivityDecision(false, "below_threshold")
        }

        val repeatThreshold = settings.reminderRepeatUnit.toDuration(settings.reminderRepeatValue)
        val canRepeat = updatedState.lastReminderAt?.let { Duration.between(it, now) >= repeatThreshold } ?: true

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

        return if (start <= end) localTime in start..<end
        else localTime !in end..<start
    }
}
