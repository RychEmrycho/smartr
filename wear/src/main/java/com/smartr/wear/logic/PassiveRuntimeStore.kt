package com.smartr.wear.logic

object PassiveRuntimeStore {
    @Volatile
    var inactivityState: InactivityState = InactivityState(
        sedentaryStart = null,
        lastMovement = null,
        lastReminderAt = null
    )

    @Volatile
    var lastDailySteps: Long? = null
}
