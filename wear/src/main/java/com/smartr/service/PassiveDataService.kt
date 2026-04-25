package com.smartr.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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

import com.smartr.data.TrackingStateRepository
import kotlinx.coroutines.flow.first

class PassiveDataService : PassiveListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        serviceScope.launch {
            val trackingRepo = TrackingStateRepository(applicationContext)
            
            // Hydrate cache if first run of this process
            if (PassiveRuntimeStore.lastPassiveCallbackAt == null) {
                val persistedState = trackingRepo.state.first()
                val persistedSteps = trackingRepo.lastDailySteps.first()
                val persistedOffBody = trackingRepo.isOffBody.first()
                PassiveRuntimeStore.updateFromPersisted(persistedState, persistedSteps, persistedOffBody)
            }

            val now = Instant.now()
            
            val latestDailySteps = dataPoints.getData(DataType.STEPS_DAILY)
                .lastOrNull()
                ?.value
            
            val movementDetected = latestDailySteps?.let { current ->
                val previous = PassiveRuntimeStore.lastDailySteps
                PassiveRuntimeStore.lastDailySteps = current
                launch { trackingRepo.updateSteps(current) }
                previous != null && current > previous
            } ?: false

            if (movementDetected) {
                android.util.Log.i("PassiveDataService", "Physical movement detected (Steps)")
            }

            val previousCallbackAt = PassiveRuntimeStore.lastPassiveCallbackAt
            PassiveRuntimeStore.lastPassiveCallbackAt = now
            val elapsedMinutes = previousCallbackAt?.let {
                Duration.between(it, now).toMinutes().coerceIn(0, 30)
            } ?: 0L

            val settingsRepo = SettingsRepository(applicationContext)
            val historyRepository = HistoryRepository(applicationContext)
            val settings = settingsRepo.currentSettings()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val isDndEnabled = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                applicationContext.registerReceiver(null, ifilter)
            }
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            if (PassiveRuntimeStore.isDndEnabled != isDndEnabled) {
                android.util.Log.i("PassiveDataService", "DND status changed: $isDndEnabled")
                PassiveRuntimeStore.isDndEnabled = isDndEnabled
            }
            if (PassiveRuntimeStore.isCharging != isCharging) {
                android.util.Log.i("PassiveDataService", "Charging status changed: $isCharging")
                PassiveRuntimeStore.isCharging = isCharging
            }

            val engine = InactivityEngine()
            
            val previousState = PassiveRuntimeStore.inactivityState
            val (updatedState, decision) = engine.evaluate(
                state = previousState,
                now = now,
                settings = settings,
                movementDetected = movementDetected,
                isWatchSleeping = PassiveRuntimeStore.isWatchSleeping,
                isOffBody = PassiveRuntimeStore.isOffBody,
                isCharging = isCharging,
                isDndEnabled = isDndEnabled,
                isExercising = PassiveRuntimeStore.isExercising
            )
            
            if (updatedState != previousState) {
                launch { trackingRepo.updateState(updatedState) }
            }

            PassiveRuntimeStore.inactivityState = updatedState
            android.util.Log.d("PassiveDataService", "Engine evaluation: ${decision.reason}")
            
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
        android.util.Log.i("PassiveDataService", "onUserActivityInfoReceived: ${info.userActivityState}")
        val isAsleep = info.userActivityState == UserActivityState.USER_ACTIVITY_ASLEEP
        val isExercising = info.userActivityState == UserActivityState.USER_ACTIVITY_EXERCISE

        var changed = false
        if (PassiveRuntimeStore.isWatchSleeping != isAsleep) {
            PassiveRuntimeStore.isWatchSleeping = isAsleep
            android.util.Log.i("PassiveDataService", "Sleep status changed: $isAsleep")
            changed = true
        }

        if (PassiveRuntimeStore.isExercising != isExercising) {
            PassiveRuntimeStore.isExercising = isExercising
            android.util.Log.i("PassiveDataService", "Exercise status changed: $isExercising")
            changed = true
        }

        if (changed) {
            serviceScope.launch {
                ComplicationUpdater.updateAll(applicationContext)
            }
        }
    }
}
