package com.smartr.logic

import java.time.Instant

object PassiveRuntimeStore {
    @Volatile
    var inactivityState: InactivityState = InactivityState(
        sedentaryStart = null,
        lastMovement = null,
        lastReminderAt = null
    )

    @Volatile
    var lastDailySteps: Long? = null

    @Volatile
    var lastPassiveCallbackAt: Instant? = null

    @Volatile
    var isWatchSleeping: Boolean = false

    @Volatile
    var isOffBody: Boolean = false

    fun reset() {
        inactivityState = inactivityState.copy(
            sedentaryStart = null,
            lastReminderAt = null
        )
    }
}
