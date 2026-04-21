package com.smartr.wear.logic

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
}
