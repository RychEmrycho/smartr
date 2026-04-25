package com.smartr.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartr.data.MobileSettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SyncPingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncPingWorker", "Starting sync ping")
            val nodes = Wearable.getNodeClient(applicationContext).connectedNodes.await()

            if (nodes.isNotEmpty()) {
                for (node in nodes) {
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(node.id, "/ping", "ping_from_mobile".toByteArray())
                        .await()
                    Log.d("SyncPingWorker", "Ping sent to node: ${node.displayName}")
                }
            } else {
                Log.w("SyncPingWorker", "No reachable nodes found for ping")
                // Still schedule next check
            }

            // Schedule next ping
            scheduleNext()

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncPingWorker", "Failed to perform sync ping", e)
            Result.retry()
        }
    }

    private suspend fun scheduleNext() {
        val repository = MobileSettingsRepository(applicationContext)
        val settings = repository.watchSettings.first()
        
        val delay = settings.pingValue.toLong()
        val unit = when (settings.pingUnit) {
            com.smartr.data.TimeIntervalUnit.SECONDS -> TimeUnit.SECONDS
            com.smartr.data.TimeIntervalUnit.MINUTES -> TimeUnit.MINUTES
            com.smartr.data.TimeIntervalUnit.HOURS -> TimeUnit.HOURS
        }

        val nextRequest = OneTimeWorkRequestBuilder<SyncPingWorker>()
            .setInitialDelay(delay, unit)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "sync_ping",
            androidx.work.ExistingWorkPolicy.REPLACE,
            nextRequest
        )
        Log.d("SyncPingWorker", "Next ping scheduled in $delay ${settings.pingUnit}")
    }
}
