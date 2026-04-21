package com.smartr.mobile.data.health

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

    suspend fun writeSedentarySession(startTime: Instant, endTime: Instant, minutes: Int) {
        if (!hasAllPermissions()) return

        val record = ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = java.time.OffsetDateTime.now().offset,
            endTime = endTime,
            endZoneOffset = java.time.OffsetDateTime.now().offset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            title = "Sedentary Period ($minutes min)",
            notes = "Recorded by Smartr"
        )

        try {
            healthConnectClient.insertRecords(listOf(record))
            Log.d("HealthConnect", "Exported sedentary session: $minutes mins")
        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to export sedentary session", e)
        }
    }

    fun getPermissionRequestIntent() = 
        PermissionController.createRequestPermissionResultContract().let {
            // This is just a placeholder to show we use the contract
        }
}
