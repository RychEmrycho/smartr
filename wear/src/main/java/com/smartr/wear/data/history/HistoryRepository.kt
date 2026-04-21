package com.smartr.wear.data.history

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HistoryRepository(context: Context) {
    private val dao = HistoryDatabase.get(context).dailySummaryDao()

    fun summaries(): Flow<List<DailySummary>> = dao.latest30Days()

    suspend fun recordReminderSent(date: LocalDate) {
        val key = date.toString()
        val existing = dao.findByDate(key)
        dao.upsert(
            DailySummary(
                dateIso = key,
                sedentaryMinutes = existing?.sedentaryMinutes ?: 0,
                remindersSent = (existing?.remindersSent ?: 0) + 1,
                remindersAcknowledged = existing?.remindersAcknowledged ?: 0
            )
        )
    }

    suspend fun recordReminderAcknowledged(date: LocalDate) {
        val key = date.toString()
        val existing = dao.findByDate(key)
        dao.upsert(
            DailySummary(
                dateIso = key,
                sedentaryMinutes = existing?.sedentaryMinutes ?: 0,
                remindersSent = existing?.remindersSent ?: 0,
                remindersAcknowledged = (existing?.remindersAcknowledged ?: 0) + 1
            )
        )
    }

    suspend fun addSedentaryMinutesSample(date: LocalDate, minutes: Int) {
        val key = date.toString()
        val existing = dao.findByDate(key)
        dao.upsert(
            DailySummary(
                dateIso = key,
                sedentaryMinutes = (existing?.sedentaryMinutes ?: 0) + minutes.coerceAtLeast(0),
                remindersSent = existing?.remindersSent ?: 0,
                remindersAcknowledged = existing?.remindersAcknowledged ?: 0
            )
        )
    }
}
