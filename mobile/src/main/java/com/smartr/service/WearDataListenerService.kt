package com.smartr.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.smartr.data.MobileSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/settings") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val thresholdValue = dataMap.getInt("sitThresholdValue")
                val thresholdUnit = dataMap.getInt("sitThresholdUnit")
                val repeatValue = dataMap.getInt("repeatValue")
                val repeatUnit = dataMap.getInt("repeatUnit")
                val quietStart = dataMap.getInt("quietStart")
                val quietEnd = dataMap.getInt("quietEnd")

                Log.d("WearDataListener", "Received settings from watch")
                
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: Settings synced from Watch", Toast.LENGTH_SHORT).show()
                }

                val repository = MobileSettingsRepository(applicationContext)
                scope.launch {
                    repository.updateFromWatch(
                        thresholdValue = thresholdValue,
                        thresholdUnit = thresholdUnit,
                        repeatValue = repeatValue,
                        repeatUnit = repeatUnit,
                        quietStart = quietStart,
                        quietEnd = quietEnd
                    )
                }
            } else if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/history") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val summaryMaps = dataMap.getDataMapArrayList("summaries") ?: emptyList()
                
                val entities = summaryMaps.map { map ->
                    com.smartr.data.history.DailySummaryEntity(
                        dateIso = map.getString("date") ?: "",
                        sedentaryMinutes = map.getInt("sedentary"),
                        remindersSent = map.getInt("sent"),
                        remindersAcknowledged = map.getInt("ack")
                    )
                }

                Log.d("WearDataListener", "Received ${entities.size} history summaries")
                
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: History synced from Watch", Toast.LENGTH_SHORT).show()
                }

                val historyRepository = com.smartr.data.history.MobileHistoryRepository(applicationContext)
                scope.launch {
                    historyRepository.recordSummaries(entities)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/ping" -> {
                Log.d("WearDataListener", "Received Ping from Watch")
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: Ping received from Watch", Toast.LENGTH_SHORT).show()
                }
                // Auto-reply with Pong
                scope.launch {
                    try {
                        Wearable.getMessageClient(applicationContext)
                            .sendMessage(messageEvent.sourceNodeId, "/pong", "mobile_reply".toByteArray())
                            .await()
                    } catch (e: Exception) {
                        Log.e("WearDataListener", "Failed to send Pong", e)
                    }
                }
            }
            "/pong" -> {
                Log.d("WearDataListener", "Received Pong from Watch")
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Smartr: Pong received from Watch", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
