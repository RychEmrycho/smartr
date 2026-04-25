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

class OffBodyService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var offBodySensor: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        offBodySensor = sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
        
        if (offBodySensor != null) {
            sensorManager.registerListener(this, offBodySensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("OffBodyService", "Sensor listener registered")
        } else {
            Log.e("OffBodyService", "Low Latency Off-Body sensor not found")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
            val isOffBody = event.values[0] == 0f // 0.0 means off-wrist, 1.0 means on-wrist
            PassiveRuntimeStore.isOffBody = isOffBody
            Log.d("OffBodyService", "Off-body state changed: $isOffBody")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("OffBodyService", "Sensor listener unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
