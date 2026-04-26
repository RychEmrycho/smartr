package com.smartr.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.UserActivityInfo
import androidx.health.services.client.data.UserActivityState
import com.smartr.complication.ComplicationUpdater
import com.smartr.data.SettingsRepository
import com.smartr.data.TrackingStateRepository
import com.smartr.data.history.HistoryRepository
import com.smartr.logic.InactivityDecision
import com.smartr.logic.InactivityEngine
import com.smartr.logic.InactivityState
import com.smartr.logic.PassiveRuntimeStore
import com.smartr.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PassiveDataService : PassiveListenerService() {
    companion object {
        private const val TAG = "PassiveDataService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        serviceScope.launch {
            val trackingRepo = TrackingStateRepository(applicationContext)
            hydrateCacheIfNeeded(trackingRepo)

            val now = Instant.now()
            val movementDetected = processSteps(dataPoints, trackingRepo)

            val elapsedSeconds = calculateElapsedSeconds(now)
            PassiveRuntimeStore.lastPassiveCallbackAt = now

            val settingsRepo = SettingsRepository(applicationContext)
            val settings = settingsRepo.currentSettings()
            
            updateSystemStatus()

            val engine = InactivityEngine()
            val previousState = PassiveRuntimeStore.inactivityState
            val (updatedState, decision) = engine.evaluate(
                state = previousState,
                now = now,
                settings = settings,
                movementDetected = movementDetected,
                isWatchSleeping = PassiveRuntimeStore.isWatchSleeping,
                isOffBody = PassiveRuntimeStore.isOffBody,
                isCharging = PassiveRuntimeStore.isCharging,
                isDndEnabled = PassiveRuntimeStore.isDndEnabled,
                isExercising = PassiveRuntimeStore.isExercising
            )
            
            if (updatedState != previousState) {
                launch { trackingRepo.updateState(updatedState) }
            }

            PassiveRuntimeStore.inactivityState = updatedState
            Log.d(TAG, "Engine evaluation: ${decision.reason}")
            
            handleDecision(decision, movementDetected, updatedState, elapsedSeconds)
            ComplicationUpdater.updateAll(applicationContext)
        }
    }

    private suspend fun hydrateCacheIfNeeded(trackingRepo: TrackingStateRepository) {
        if (PassiveRuntimeStore.lastPassiveCallbackAt == null) {
            val persistedState = trackingRepo.state.first()
            val persistedSteps = trackingRepo.lastDailySteps.first()
            val persistedOffBody = trackingRepo.isOffBody.first()
            PassiveRuntimeStore.updateFromPersisted(persistedState, persistedSteps, persistedOffBody)
        }
    }

    private suspend fun processSteps(dataPoints: DataPointContainer, trackingRepo: TrackingStateRepository): Boolean {
        val stepDataPoints = dataPoints.getData(DataType.STEPS_DAILY)
        val currentSteps = stepDataPoints.lastOrNull()?.value
        val previousSteps = PassiveRuntimeStore.lastDailySteps
        
        currentSteps?.let { 
            PassiveRuntimeStore.lastDailySteps = it
            trackingRepo.updateSteps(it)
        }

        val movementDetected = currentSteps != null && previousSteps != null && currentSteps > previousSteps
        if (movementDetected) {
            Log.i(TAG, "Physical movement detected (Steps)")
        }
        return movementDetected
    }

    private fun calculateElapsedSeconds(now: Instant): Long {
        val previousCallbackAt = PassiveRuntimeStore.lastPassiveCallbackAt
        return previousCallbackAt?.let {
            Duration.between(it, now).toSeconds()
        } ?: 0L
    }

    private fun updateSystemStatus() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val isDndEnabled = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        if (PassiveRuntimeStore.isDndEnabled != isDndEnabled) {
            Log.i(TAG, "DND status changed: $isDndEnabled")
            PassiveRuntimeStore.isDndEnabled = isDndEnabled
        }
        if (PassiveRuntimeStore.isCharging != isCharging) {
            Log.i(TAG, "Charging status changed: $isCharging")
            PassiveRuntimeStore.isCharging = isCharging
        }
    }

    private suspend fun handleDecision(
        decision: InactivityDecision,
        movementDetected: Boolean,
        updatedState: InactivityState,
        elapsedSeconds: Long
    ) {
        val historyRepository = HistoryRepository(applicationContext)
        
        if (!movementDetected && updatedState.sedentaryStart != null && elapsedSeconds > 0) {
            historyRepository.addSedentaryMinutesSample(
                LocalDate.now(ZoneId.systemDefault()),
                seconds = elapsedSeconds.toInt()
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
    }

    override fun onUserActivityInfoReceived(info: UserActivityInfo) {
        Log.i(TAG, "onUserActivityInfoReceived: ${info.userActivityState}")
        val isAsleep = info.userActivityState == UserActivityState.USER_ACTIVITY_ASLEEP
        val isExercising = info.userActivityState == UserActivityState.USER_ACTIVITY_EXERCISE

        var changed = false
        if (PassiveRuntimeStore.isWatchSleeping != isAsleep) {
            PassiveRuntimeStore.isWatchSleeping = isAsleep
            Log.i(TAG, "Sleep status changed: $isAsleep")
            changed = true
        }

        if (PassiveRuntimeStore.isExercising != isExercising) {
            PassiveRuntimeStore.isExercising = isExercising
            Log.i(TAG, "Exercise status changed: $isExercising")
            changed = true
        }

        if (changed) {
            serviceScope.launch {
                ComplicationUpdater.updateAll(applicationContext)
            }
        }
    }
}
