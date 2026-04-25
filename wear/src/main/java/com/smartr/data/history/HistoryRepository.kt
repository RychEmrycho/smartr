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
        ensureDayExists(key)
        dao.incrementSent(key)
        syncAll()
    }

    suspend fun recordReminderAcknowledged(date: LocalDate) {
        val key = date.toString()
        ensureDayExists(key)
        dao.incrementAcknowledged(key)
        syncAll()
    }

    suspend fun addSedentaryMinutesSample(date: LocalDate, minutes: Int) {
        if (minutes <= 0) return
        val key = date.toString()
        ensureDayExists(key)
        dao.addMinutes(key, minutes)
        syncAll()
    }

    private suspend fun ensureDayExists(dateIso: String) {
        if (dao.findByDate(dateIso) == null) {
            dao.upsert(DailySummary(dateIso, 0, 0, 0))
        }
    }
}
