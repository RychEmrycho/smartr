package com.smartr.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearSyncManager(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    suspend fun syncSettings(settings: AppSettings) {
        try {
            val request = PutDataMapRequest.create("/settings").run {
                dataMap.putInt("sitThresholdValue", settings.sitThresholdValue)
                dataMap.putInt("sitThresholdUnit", settings.sitThresholdUnit.ordinal)
                dataMap.putInt("repeatValue", settings.reminderRepeatValue)
                dataMap.putInt("repeatUnit", settings.reminderRepeatUnit.ordinal)
                dataMap.putInt("quietStart", settings.quietStartHour)
                dataMap.putInt("quietEnd", settings.quietEndHour)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                asPutDataRequest()
            }
            dataClient.putDataItem(request).await()
            Log.d("WearSyncManager", "Settings synced to Data Layer: $settings")
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Failed to sync settings", e)
        }
    }

    suspend fun syncHistory(summaries: List<com.smartr.data.history.DailySummary>) {
        try {
            val request = PutDataMapRequest.create("/history").run {
                val dataMaps = summaries.map { summary ->
                    com.google.android.gms.wearable.DataMap().apply {
                        putString("date", summary.dateIso)
                        putInt("sedentary", summary.sedentarySeconds)
                        putInt("sent", summary.remindersSent)
                        putInt("ack", summary.remindersAcknowledged)
                    }
                }
                dataMap.putDataMapArrayList("summaries", ArrayList(dataMaps))
                dataMap.putLong("timestamp", System.currentTimeMillis())
                setUrgent()
                asPutDataRequest()
            }
            dataClient.putDataItem(request).await()
            Log.d("WearSyncManager", "History synced to Data Layer: ${summaries.size} items")
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Failed to sync history", e)
        }
    }
}
