package com.smartr.wear.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.smartr.wear.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/status/sleep") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val isSleeping = dataMap.getBoolean("isSleeping")
                
                Log.d("PhoneDataListener", "Received sleep status from phone: $isSleeping")
                
                scope.launch {
                    settingsRepository.updateSleepStatus(isSleeping)
                }
            }
        }
    }
}
