package com.smartr.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.smartr.logic.PassiveRuntimeStore

import android.content.BroadcastReceiver
import android.widget.Toast

class OffBodyService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var offBodySensor: Sensor? = null

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.smartr.DEBUG_OFF_BODY") {
                val isOffBody = intent.getBooleanExtra("isOffBody", false)
                updateState(isOffBody, "DEBUG")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Register debug receiver
        val filter = android.content.IntentFilter("com.smartr.DEBUG_OFF_BODY")
        registerReceiver(debugReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // Try to find the best available off-body sensor
        // 34 = TYPE_LOW_LATENCY_OFFBODY_DETECT (API 34+)
        // 23 = TYPE_OFFBODY_DETECT (Older devices/System API)
        offBodySensor = sensorManager.getDefaultSensor(34) ?: sensorManager.getDefaultSensor(23)
        
        // If still not found, search the full list for anything containing "offbody"
        if (offBodySensor == null) {
            val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            Log.d("OffBodyService", "--- START SENSOR DUMP ---")
            allSensors.forEach { 
                Log.d("OffBodyService", "Sensor: ${it.name} | Type: ${it.type} | StringType: ${it.stringType}")
            }
            Log.d("OffBodyService", "--- END SENSOR DUMP ---")
            
            // Only look for "offbody" now, ignore "wrist" as it picks up tilt sensors on emulator
            offBodySensor = allSensors.firstOrNull { 
                it.stringType.contains("offbody", ignoreCase = true) ||
                it.name.contains("off-body", ignoreCase = true)
            }
        }
        
        if (offBodySensor != null) {
            sensorManager.registerListener(this, offBodySensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("OffBodyService", "Sensor listener registered: ${offBodySensor?.name} (${offBodySensor?.type})")
        } else {
            Log.e("OffBodyService", "No Off-Body sensor found on this device")
        }
    }

    private fun updateState(isOffBody: Boolean, source: String) {
        val previousState = PassiveRuntimeStore.isOffBody
        if (previousState != isOffBody) {
            PassiveRuntimeStore.isOffBody = isOffBody
            val message = if (isOffBody) "Watch Removed (Paused)" else "Watch On-Wrist (Active)"
            Log.i("OffBodyService", "EVENT [$source]: $message")
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Handle any sensor that we've identified as an off-body sensor
        if (event.sensor.type == offBodySensor?.type) {
            val isOffBody = event.values[0] == 0f // 0.0 means off-wrist, 1.0 means on-wrist
            updateState(isOffBody, "SENSOR")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(debugReceiver)
        sensorManager.unregisterListener(this)
        Log.d("OffBodyService", "Sensor listener unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
