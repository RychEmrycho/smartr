package com.smartr.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class MobileSyncManager(context: Context) {
    private val dataClient = Wearable.getDataClient(context)

    suspend fun syncSleepStatus(isSleeping: Boolean) {
        try {
            val request = PutDataMapRequest.create("/status/sleep").run {
                dataMap.putBoolean("isSleeping", isSleeping)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                setUrgent()
                asPutDataRequest()
            }
            dataClient.putDataItem(request).await()
            Log.d("MobileSyncManager", "Sleep status synced: $isSleeping")
        } catch (e: Exception) {
            Log.e("MobileSyncManager", "Failed to sync sleep status", e)
        }
    }
}
