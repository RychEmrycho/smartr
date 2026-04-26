package com.smartr.logic

import android.util.Log
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
    val reason: String,
    val isMovementReset: Boolean = false
)

class InactivityEngine(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    companion object {
        private const val TAG = "InactivityEngine"
    }

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
        Log.v(TAG, "evaluate() called with movementDetected=$movementDetected")
        val bufferThreshold = settings.movementBufferUnit.toDuration(settings.movementBufferValue)
        var currentState = state

        // 1. Check for conditions that suppress tracking/reminders
        checkSuppression(currentState, now, isOffBody, isCharging, isExercising)?.let {
            return it.also { Log.d(TAG, "evaluate: suppressed by ${it.second.reason}") }
        }

        // 2. Process movement if detected
        if (movementDetected) {
            val (movedState, decision) = processMovement(currentState, now, bufferThreshold)
            currentState = movedState
            // Only exit early if the movement was significant enough to reset the sedentary timer
            if (decision.isMovementReset) {
                return currentState to decision.also { Log.d(TAG, "evaluate: movement reset") }
            }
            Log.v(TAG, "evaluate: short movement detected, continuing evaluation")
        } else {
            // 3. Handle grace period for movement start time
            currentState = handleMovementGracePeriod(currentState, now, bufferThreshold)
        }

        // 4. Check for conditions that suppress reminders (but keep tracking)
        checkReminderSuppression(currentState, now, settings, isWatchSleeping, isDndEnabled)?.let {
            return it.also { Log.d(TAG, "evaluate: reminder suppressed by ${it.second.reason}") }
        }

        // 5. Evaluate sedentary duration
        return evaluateSedentaryThreshold(currentState, now, settings).also {
            if (it.second.shouldRemind) {
                Log.i(TAG, "evaluate: REMINDER TRIGGERED: ${it.second.reason}")
            }
        }
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

        val result = if (movementDuration < bufferThreshold) {
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
            ) to InactivityDecision(false, "movement_reset", isMovementReset = true)
        }
        Log.d(TAG, "Process movement: ${result.second.reason} (duration: ${movementDuration.seconds}s, threshold: ${bufferThreshold.seconds}s)")
        return result
    }

    private fun handleMovementGracePeriod(
        state: InactivityState,
        now: Instant,
        bufferThreshold: Duration
    ): InactivityState {
        val timeSinceLastMove = state.lastMovement?.let { Duration.between(it, now) } ?: Duration.ZERO
        return if (state.lastSignificantMovementAt != null && timeSinceLastMove > bufferThreshold) {
            Log.d(TAG, "Movement grace period expired, clearing lastSignificantMovementAt")
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
        val suppression = when {
            settings.isSleeping || isWatchSleeping -> state to InactivityDecision(false, "user_sleeping")
            isDndEnabled -> state to InactivityDecision(false, "dnd_enabled")
            isQuietHours(now, settings) -> {
                val quietState = state.sedentaryStart?.let { state } ?: state.copy(sedentaryStart = now)
                quietState to InactivityDecision(false, "quiet_hours")
            }
            else -> null
        }
        if (suppression != null) {
            Log.d(TAG, "Reminder suppression active: ${suppression.second.reason}")
        }
        return suppression
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
            Log.v(TAG, "Below sedentary threshold: ${sedentaryDuration.toSeconds()}s / ${sitThreshold.toSeconds()}s")
            return updatedState to InactivityDecision(false, "below_threshold")
        }

        val repeatThreshold = settings.reminderRepeatUnit.toDuration(settings.reminderRepeatValue)
        val canRepeat = updatedState.lastReminderAt?.let { Duration.between(it, now) >= repeatThreshold } ?: true

        val result = if (canRepeat) {
            updatedState.copy(lastReminderAt = now) to InactivityDecision(true, "threshold_reached")
        } else {
            updatedState to InactivityDecision(false, "repeat_window")
        }
        Log.d(TAG, "Evaluate sedentary: ${result.second.reason} (duration: ${sedentaryDuration.toSeconds()}s)")
        return result
    }

    private fun isQuietHours(now: Instant, settings: AppSettings): Boolean {
        val localTime = now.atZone(zoneId).toLocalTime()
        val start = LocalTime.of(settings.quietStartHour, 0)
        val end = LocalTime.of(settings.quietEndHour, 0)

        val isQuiet = if (start <= end) localTime in start..<end
        else localTime !in end..<start
        
        Log.v(TAG, "isQuietHours: $isQuiet (Current: $localTime, Start: $start, End: $end)")
        return isQuiet
    }
}
