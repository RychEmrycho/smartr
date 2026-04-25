package com.smartr.service

import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState
import com.smartr.data.SettingsRepository
import com.smartr.data.history.HistoryRepository
import com.smartr.complication.ComplicationUpdater
import com.smartr.logic.InactivityEngine
import com.smartr.logic.PassiveRuntimeStore
import com.smartr.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PassiveDataService : PassiveListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        serviceScope.launch {
            val now = Instant.now()
            val latestDailySteps = dataPoints.getData(DataType.STEPS_DAILY)
                .lastOrNull()
                ?.value
            val movementDetected = latestDailySteps?.let { current ->
                val previous = PassiveRuntimeStore.lastDailySteps
                PassiveRuntimeStore.lastDailySteps = current
                previous != null && current > previous
            } ?: false

            val previousCallbackAt = PassiveRuntimeStore.lastPassiveCallbackAt
            PassiveRuntimeStore.lastPassiveCallbackAt = now
            val elapsedMinutes = previousCallbackAt?.let {
                Duration.between(it, now).toMinutes().coerceIn(0, 30)
            } ?: 0L

            val settingsRepo = SettingsRepository(applicationContext)
            val historyRepository = HistoryRepository(applicationContext)
            val settings = settingsRepo.currentSettings()
            val engine = InactivityEngine()
            val (updatedState, decision) = engine.evaluate(
                state = PassiveRuntimeStore.inactivityState,
                now = now,
                settings = settings,
                movementDetected = movementDetected,
                isWatchSleeping = PassiveRuntimeStore.isWatchSleeping
            )
            PassiveRuntimeStore.inactivityState = updatedState
            if (!movementDetected && updatedState.sedentaryStart != null && elapsedMinutes > 0) {
                historyRepository.addSedentaryMinutesSample(
                    LocalDate.now(ZoneId.systemDefault()),
                    minutes = elapsedMinutes.toInt()
                )
            }

            if (decision.shouldRemind) {
                val scheduler = ReminderScheduler(applicationContext)
                scheduler.ensureChannel()
                scheduler.sendReminder()
                historyRepository.recordReminderSent(
                    LocalDate.now(ZoneId.systemDefault())
                )
            }
            ComplicationUpdater.updateAll(applicationContext)
        }
    }

    override fun onUserActivityInfoReceived(info: UserActivityInfo) {
        val isAsleep = info.userActivityState == UserActivityState.USER_ACTIVITY_ASLEEP
        if (PassiveRuntimeStore.isWatchSleeping != isAsleep) {
            PassiveRuntimeStore.isWatchSleeping = isAsleep
            android.util.Log.d("PassiveDataService", "Local sleep status changed: $isAsleep")
            
            // Trigger a UI and complication update immediately when sleep status changes
            serviceScope.launch {
                ComplicationUpdater.updateAll(applicationContext)
            }
        }
    }
}
