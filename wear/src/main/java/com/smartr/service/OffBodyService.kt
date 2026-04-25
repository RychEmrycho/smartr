package com.smartr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smartr.R
import com.smartr.data.TrackingStateRepository
import com.smartr.logic.PassiveRuntimeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that monitors whether the watch is being worn or is off-wrist.
 * Updates [PassiveRuntimeStore] and [TrackingStateRepository] to ensure tracking is paused
 * when the device is not on the user's wrist.
 */
class OffBodyService : Service(), SensorEventListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private lateinit var trackingRepository: TrackingStateRepository
    
    private var offBodySensor: Sensor? = null
    private var firstEventReceived = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        trackingRepository = TrackingStateRepository(applicationContext)

        initializeStateSync()
        startForegroundWithNotification()
        setupOffBodySensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- State & Persistence ---

    /**
     * Syncs the in-memory [PassiveRuntimeStore] with the persisted state from [TrackingStateRepository].
     */
    private fun initializeStateSync() {
        serviceScope.launch {
            trackingRepository.isOffBody.collectLatest { isOffBody ->
                PassiveRuntimeStore.isOffBody = isOffBody
            }
        }
    }

    /**
     * Updates the off-body state in both memory and persistent storage.
     */
    private fun updateState(isOffBody: Boolean) {
        val previousState = PassiveRuntimeStore.isOffBody
        
        // Only update if state changed OR it's the first event after service start
        if (!firstEventReceived || previousState != isOffBody) {
            firstEventReceived = true
            PassiveRuntimeStore.isOffBody = isOffBody
            
            Log.i(TAG, "Watch status changed: ${if (isOffBody) "Off-wrist" else "On-wrist"}")
            
            serviceScope.launch {
                trackingRepository.setOffBody(isOffBody)
            }
        }
    }

    // --- Sensor Management ---

    private fun setupOffBodySensor() {
        offBodySensor = findOffBodySensor()
        
        if (offBodySensor != null) {
            val registered = sensorManager.registerListener(
                this, 
                offBodySensor, 
                SensorManager.SENSOR_DELAY_NORMAL
            )
            if (registered) {
                Log.d(TAG, "Off-body sensor registered: ${offBodySensor?.name}")
            } else {
                Log.e(TAG, "Failed to register off-body sensor listener")
            }
        } else {
            Log.w(TAG, "No Off-Body sensor found on this device")
        }
    }

    private fun findOffBodySensor(): Sensor? {
        // 1. Try modern API 35 low latency sensor
        val lowLatency = sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
        if (lowLatency != null) return lowLatency

        // 2. Try standard off-body detect (Type 23)
        val standard = sensorManager.getDefaultSensor(TYPE_OFFBODY_DETECT)
        if (standard != null) return standard

        // 3. Fallback: Search all sensors for offbody keyword
        return sensorManager.getSensorList(Sensor.TYPE_ALL).firstOrNull { 
            val typeStr = it.stringType.lowercase()
            val nameStr = it.name.lowercase()
            typeStr.contains("offbody") || nameStr.contains("off-body") || nameStr.contains("offbody")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == offBodySensor?.type) {
            // Values: 0.0 = off-wrist, 1.0 = on-wrist
            val isOffBody = event.values[0] == 0f
            updateState(isOffBody)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    // --- Notification & Foreground ---

    private fun startForegroundWithNotification() {
        createNotificationChannel()
        val notification = buildForegroundNotification()

        startForeground(
            NOTIFICATION_ID, 
            notification, 
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.off_body_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.off_body_service_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.off_body_service_notification_title))
            .setContentText(getString(R.string.off_body_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_info_details) // TODO: Replace with app icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "OffBodyService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "off_body_detection"
        private const val TYPE_OFFBODY_DETECT = 23 // Constant for Sensor.TYPE_OFFBODY_DETECT
    }
}
