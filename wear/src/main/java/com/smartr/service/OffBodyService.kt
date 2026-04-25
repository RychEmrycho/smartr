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

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Try to find the best available off-body sensor
        // 34 = TYPE_LOW_LATENCY_OFFBODY_DETECT (API 34+)
        // 23 = TYPE_OFFBODY_DETECT (Older devices/System API)
        offBodySensor = sensorManager.getDefaultSensor(34) ?: sensorManager.getDefaultSensor(23)
        
        // If still not found, search the full list for anything containing "offbody"
        if (offBodySensor == null) {
            val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            offBodySensor = allSensors.firstOrNull { 
                it.stringType.contains("offbody", ignoreCase = true) ||
                it.name.contains("off-body", ignoreCase = true)
            }
        }
        
        if (offBodySensor != null) {
            sensorManager.registerListener(this, offBodySensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("OffBodyService", "Off-body sensor registered: ${offBodySensor?.name}")
        } else {
            Log.w("OffBodyService", "No Off-Body sensor found on this device")
        }
    }

    private fun updateState(isOffBody: Boolean) {
        val previousState = PassiveRuntimeStore.isOffBody
        if (previousState != isOffBody) {
            PassiveRuntimeStore.isOffBody = isOffBody
            Log.i("OffBodyService", "Watch status changed: ${if (isOffBody) "Off-wrist" else "On-wrist"}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == offBodySensor?.type) {
            val isOffBody = event.values[0] == 0f // 0.0 means off-wrist, 1.0 means on-wrist
            updateState(isOffBody)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
