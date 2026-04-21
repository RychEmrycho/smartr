package com.smartr.wear.data

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
                dataMap.putInt("sitThreshold", settings.sitThresholdMinutes)
                dataMap.putInt("repeatMinutes", settings.reminderRepeatMinutes)
                dataMap.putInt("quietStart", settings.quietStartHour)
                dataMap.putInt("quietEnd", settings.quietEndHour)
                // Add a timestamp to ensure data is always updated
                dataMap.putLong("timestamp", System.currentTimeMillis())
                asPutDataRequest()
            }
            dataClient.putDataItem(request).await()
            Log.d("WearSyncManager", "Settings synced to Data Layer: $settings")
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Failed to sync settings", e)
        }
    }
}
