package com.smartr.logic

import com.smartr.data.AppSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class InactivityState(
    val sedentaryStart: Instant?,
    val lastMovement: Instant?,
    val lastReminderAt: Instant?
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
        isWatchSleeping: Boolean = false
    ): Pair<InactivityState, InactivityDecision> {
        if (movementDetected) {
            return state.copy(
                sedentaryStart = null,
                lastMovement = now,
                lastReminderAt = null
            ) to InactivityDecision(false, "movement_reset")
        }

        if (settings.isSleeping || isWatchSleeping) {
            return state to InactivityDecision(false, "user_sleeping")
        }

        if (isQuietHours(now, settings)) {
            val quietState = if (state.sedentaryStart == null) state.copy(sedentaryStart = now) else state
            return quietState to InactivityDecision(false, "quiet_hours")
        }

        val updatedState = if (state.sedentaryStart == null) state.copy(sedentaryStart = now) else state
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
