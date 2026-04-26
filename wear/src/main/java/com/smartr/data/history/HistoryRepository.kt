package com.smartr.data.history

import android.content.Context
import com.smartr.data.WearSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

import java.time.LocalTime

class HistoryRepository(private val context: Context) {
    private val database = HistoryDatabase.get(context)
    private val dao = database.dailySummaryDao()
    private val pbDao = database.personalBestDao()
    private val syncManager = WearSyncManager(context)

    fun summaries(): Flow<List<DailySummary>> = dao.latest30Days()

    fun personalBests(): Flow<List<PersonalBest>> = pbDao.getAll()

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
        
        // Update total
        dao.addMinutes(key, minutes)
        
        // Update hourly
        val currentSummary = dao.findByDate(key)
        currentSummary?.let { summary ->
            val hour = LocalTime.now().hour
            val updatedHourly = summary.hourlySedentary.toMutableList()
            updatedHourly[hour] = (updatedHourly[hour] + minutes).coerceAtMost(60)
            dao.upsert(summary.copy(hourlySedentary = updatedHourly))
        }

        syncAll()
    }

    suspend fun updatePersonalBest(type: String, value: Int, date: LocalDate) {
        val existing = pbDao.findByType(type)
        if (existing == null || isRecordBetter(type, value, existing.value)) {
            pbDao.upsert(PersonalBest(type, value, date.toString()))
        }
    }

    private fun isRecordBetter(type: String, newValue: Int, oldValue: Int): Boolean {
        return when (type) {
            "min_sedentary" -> newValue < oldValue
            else -> newValue > oldValue
        }
    }

    private suspend fun ensureDayExists(dateIso: String) {
        if (dao.findByDate(dateIso) == null) {
            dao.upsert(DailySummary(dateIso, 0, 0, 0, List(24) { 0 }))
        }
    }
}
