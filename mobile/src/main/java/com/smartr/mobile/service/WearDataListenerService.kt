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
                val sitThreshold = dataMap.getInt("sitThreshold")
                val repeatMinutes = dataMap.getInt("repeatMinutes")
                val quietStart = dataMap.getInt("quietStart")
                val quietEnd = dataMap.getInt("quietEnd")

                Log.d("WearDataListener", "Received settings from watch: SitLimit=$sitThreshold")

                val repository = MobileSettingsRepository(applicationContext)
                scope.launch {
                    repository.updateFromWatch(
                        sitThreshold = sitThreshold,
                        repeatMinutes = repeatMinutes,
                        quietStart = quietStart,
                        quietEnd = quietEnd
                    )
                }
            }
        }
    }
}
