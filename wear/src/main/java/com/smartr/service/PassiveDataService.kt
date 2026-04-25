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

            if (movementDetected) {
                android.util.Log.i("PassiveDataService", "EVENT: Physical movement detected (Steps)")
            }

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
                isWatchSleeping = PassiveRuntimeStore.isWatchSleeping,
                isOffBody = PassiveRuntimeStore.isOffBody
            )
            
            // Log engine decision
            if (decision.reason != "monitoring") {
                android.util.Log.i("PassiveDataService", "ENGINE DECISION: ${decision.reason} (ShouldRemind: ${decision.shouldRemind})")
            }

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
        val activityName = when (info.userActivityState) {
            UserActivityState.USER_ACTIVITY_ASLEEP -> "Asleep"
            UserActivityState.USER_ACTIVITY_PASSIVE -> "Passive/Still"
            UserActivityState.USER_ACTIVITY_EXERCISE -> "Active/Exercise"
            else -> "Unknown"
        }

        if (PassiveRuntimeStore.isWatchSleeping != isAsleep) {
            PassiveRuntimeStore.isWatchSleeping = isAsleep
            val message = if (isAsleep) "User is Asleep (Nudges Paused)" else "User is Awake"
            android.util.Log.i("PassiveDataService", "EVENT: $message")
            
            serviceScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_SHORT).show()
                ComplicationUpdater.updateAll(applicationContext)
            }
        } else {
            android.util.Log.d("PassiveDataService", "Activity Info: $activityName")
        }
    }
}
