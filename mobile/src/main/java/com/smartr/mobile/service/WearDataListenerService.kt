package com.smartr.mobile.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.smartr.mobile.data.MobileSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/settings") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val thresholdValue = dataMap.getInt("thresholdValue")
                val thresholdUnit = dataMap.getInt("thresholdUnit")
                val repeatValue = dataMap.getInt("repeatValue")
                val repeatUnit = dataMap.getInt("repeatUnit")
                val quietStart = dataMap.getInt("quietStart")
                val quietEnd = dataMap.getInt("quietEnd")

                Log.d("WearDataListener", "Received settings from watch")

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
                    com.smartr.mobile.data.history.DailySummaryEntity(
                        dateIso = map.getString("date") ?: "",
                        sedentaryMinutes = map.getInt("sedentary"),
                        remindersSent = map.getInt("sent"),
                        remindersAcknowledged = map.getInt("ack")
                    )
                }

                Log.d("WearDataListener", "Received ${entities.size} history summaries")

                val healthManager = com.smartr.mobile.data.health.HealthConnectManager(applicationContext)
                val historyRepository = com.smartr.mobile.data.history.MobileHistoryRepository(applicationContext)
                scope.launch {
                    historyRepository.recordSummaries(entities)

                    // Export most recent summary to Health Connect
                    entities.firstOrNull()?.let { summary ->
                        if (summary.sedentaryMinutes > 0) {
                            val now = java.time.Instant.now()
                            healthManager.writeSedentarySession(
                                startTime = now.minusSeconds(summary.sedentaryMinutes * 60L),
                                endTime = now,
                                minutes = summary.sedentaryMinutes
                            )
                        }
                    }
                }
            }
        }
    }
}
