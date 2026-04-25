package com.smartr.data.history

import android.content.Context
import com.smartr.data.WearSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class HistoryRepository(private val context: Context) {
    private val dao = HistoryDatabase.get(context).dailySummaryDao()
    private val syncManager = WearSyncManager(context)

    fun summaries(): Flow<List<DailySummary>> = dao.latest30Days()

    private suspend fun syncAll() {
        val latest: List<DailySummary> = summaries().first()
        syncManager.syncHistory(latest)
    }

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
        syncAll()
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
        syncAll()
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
        syncAll()
    }
}
