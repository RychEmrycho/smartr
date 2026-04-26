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

    suspend fun addSedentaryMinutesSample(date: LocalDate, seconds: Int) {
        if (seconds <= 0) return
        val key = date.toString()
        ensureDayExists(key)
        
        // Update total
        dao.addSeconds(key, seconds)
        
        // Update hourly
        val currentSummary = dao.findByDate(key)
        currentSummary?.let { summary ->
            val hour = LocalTime.now().hour
            val updatedHourly = summary.hourlySedentarySeconds.toMutableList()
            updatedHourly[hour] = (updatedHourly[hour] + seconds).coerceAtMost(3600)
            dao.upsert(summary.copy(hourlySedentarySeconds = updatedHourly))
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

    suspend fun injectMockScenario(scenario: String) {
        val today = LocalDate.now()
        val summaries = mutableListOf<DailySummary>()
        
        // Generate 14 days of data
        for (i in 0..14) {
            val date = today.minusDays(i.toLong())
            val dateIso = date.toString()
            
            val isLastWeek = i > 7
            val baseSittingSec = 300 * 60
            
            val sittingSec = when (scenario) {
                "BETTER" -> if (isLastWeek) 450 * 60 else 250 * 60
                "WORSE" -> if (isLastWeek) 200 * 60 else 500 * 60
                else -> baseSittingSec + (-50..50).random() * 60
            }
            
            val hourly = MutableList(24) { 0 }
            // Simulate peak at 2 PM (14:00)
            hourly[14] = (sittingSec / 6).coerceAtMost(3600)
            hourly[15] = (sittingSec / 6).coerceAtMost(3600)
            
            summaries.add(DailySummary(
                dateIso = dateIso,
                sedentarySeconds = sittingSec,
                remindersSent = 10,
                remindersAcknowledged = if (scenario == "BETTER") 9 else 4,
                hourlySedentarySeconds = hourly
            ))
        }
        
        summaries.forEach { dao.upsert(it) }
        
        // Mock Personal Bests
        pbDao.upsert(PersonalBest("max_streak", 12, today.minusDays(5).toString()))
        pbDao.upsert(PersonalBest("min_sedentary", 7200, today.minusDays(10).toString()))
        pbDao.upsert(PersonalBest("max_response_rate", 100, today.minusDays(2).toString()))
        
        syncAll()
    }
}
