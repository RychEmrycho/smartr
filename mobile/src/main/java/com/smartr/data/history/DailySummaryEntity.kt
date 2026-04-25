package com.smartr.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val dateIso: String,
    val sedentaryMinutes: Int,
    val remindersSent: Int,
    val remindersAcknowledged: Int
)
