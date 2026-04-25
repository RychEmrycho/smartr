package com.smartr.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.smartr.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

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
                
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: Sleep status synced", Toast.LENGTH_SHORT).show()
                }

                scope.launch {
                    settingsRepository.updateSleepStatus(isSleeping)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/ping" -> {
                Log.d("PhoneDataListener", "Received Ping from Phone")
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: Ping received", Toast.LENGTH_SHORT).show()
                }
                // Auto-reply with Pong
                scope.launch {
                    try {
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(messageEvent.sourceNodeId, "/pong", "wear_reply".toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e("PhoneDataListener", "Failed to send Pong", e)
                    }
                }
            }
            "/pong" -> {
                Log.d("PhoneDataListener", "Received Pong from Phone")
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: Pong received from Phone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
