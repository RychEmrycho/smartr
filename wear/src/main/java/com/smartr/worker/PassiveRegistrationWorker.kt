package com.smartr.worker

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartr.service.PassiveDataService
import kotlinx.coroutines.guava.await

class PassiveRegistrationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            val client = HealthServices.getClient(applicationContext).passiveMonitoringClient
            val capabilities = client.getCapabilitiesAsync().await()
            val requestedTypes = buildSet {
                if (capabilities.supportedDataTypesPassiveMonitoring.contains(DataType.STEPS)) {
                    add(DataType.STEPS)
                }
                if (capabilities.supportedDataTypesPassiveMonitoring.contains(DataType.STEPS_DAILY)) {
                    add(DataType.STEPS_DAILY)
                }
                if (capabilities.supportedDataTypesPassiveMonitoring.contains(DataType.HEART_RATE_BPM)) {
                    add(DataType.HEART_RATE_BPM)
                }
            }

            if (requestedTypes.isEmpty()) {
                return Result.success()
            }

            val config = PassiveListenerConfig.builder()
                .setDataTypes(requestedTypes)
                .setShouldUserActivityInfoBeRequested(true)
                .build()

            android.util.Log.d("PassiveRegistrationWorker", "Registering PassiveListener with types: $requestedTypes")
            client.setPassiveListenerServiceAsync(
                PassiveDataService::class.java,
                config
            ).await()
            android.util.Log.i("PassiveRegistrationWorker", "SUCCESS: PassiveListenerService registered")
            Result.success()
        }.getOrElse {
            android.util.Log.e("PassiveRegistrationWorker", "FAILURE: Failed to register PassiveListener", it)
            Result.retry()
        }
    }
}
