package com.smartr.logic

import java.time.Instant

object PassiveRuntimeStore {
    // In-memory cache for fast access during service callbacks
    @Volatile
    var inactivityState: InactivityState = InactivityState(
        sedentaryStart = null,
        lastMovement = null,
        lastReminderAt = null,
        lastSignificantMovementAt = null
    )

    @Volatile
    var lastDailySteps: Long? = null

    @Volatile
    var lastPassiveCallbackAt: Instant? = null

    @Volatile
    var isWatchSleeping: Boolean = false

    @Volatile
    var isOffBody: Boolean = false

    @Volatile
    var isCharging: Boolean = false

    @Volatile
    var isDndEnabled: Boolean = false

    @Volatile
    var isExercising: Boolean = false

    // Initialize from persisted state
    fun updateFromPersisted(
        state: InactivityState,
        steps: Long?,
        offBody: Boolean
    ) {
        inactivityState = state
        lastDailySteps = steps
        isOffBody = offBody
    }

    fun reset() {
        inactivityState = inactivityState.copy(
            sedentaryStart = null,
            lastReminderAt = null
        )
    }
}
