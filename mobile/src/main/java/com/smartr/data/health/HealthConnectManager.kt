package com.smartr.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    suspend fun isUserSleeping(): Boolean {
        if (!hasAllPermissions()) return false

        val now = Instant.now()
        val startOfToday = ZonedDateTime.now().minusHours(24).toInstant()

        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startOfToday, now)
        )

        val response = healthConnectClient.readRecords(request)
        // Check if any sleep session is currently active (end time is after now, or very recent)
        return response.records.any { it.endTime.isAfter(now.minusSeconds(300)) }
    }

    fun getPermissionRequestIntent() = 
        PermissionController.createRequestPermissionResultContract().let {
            // This is just a placeholder to show we use the contract
        }
}
