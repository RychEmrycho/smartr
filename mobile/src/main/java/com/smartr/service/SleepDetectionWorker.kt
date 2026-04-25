package com.smartr.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartr.data.MobileSettingsRepository
import com.smartr.data.MobileSyncManager
import com.smartr.data.health.HealthConnectManager

class SleepDetectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val healthManager = HealthConnectManager(context)
    private val repository = MobileSettingsRepository(context)
    private val syncManager = MobileSyncManager(context)

    override suspend fun doWork(): Result {
        try {
            val isSleeping = healthManager.isUserSleeping()
            Log.d("SleepWorker", "Polled Health Connect. User sleeping: $isSleeping")
            repository.updateSleepStatus(isSleeping)
            syncManager.syncSleepStatus(isSleeping)
            return Result.success()
        } catch (e: Exception) {
            Log.e("SleepWorker", "Failed to check sleep status", e)
            return Result.retry()
        }
    }
}
