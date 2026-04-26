package com.smartr.data.history

import android.content.Context
import com.smartr.data.WearSyncManager
import com.smartr.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalTime

class HistoryRepository(private val context: Context) {
    private val database = HistoryDatabase.get(context)
    private val dao = database.dailySummaryDao()
    private val pbDao = database.personalBestDao()
    private val eventDao = database.sedentaryEventDao()
    private val syncManager = WearSyncManager(context)
    private val settingsRepository = SettingsRepository(context)

    fun summaries(): Flow<List<DailySummary>> = dao.latest30Days()

    fun personalBests(): Flow<List<PersonalBest>> = pbDao.getAll()

    fun eventsForDay(date: LocalDate): Flow<List<SedentaryEvent>> = eventDao.eventsForDay(date.toString())

    private suspend fun syncAll() {
        val latest: List<DailySummary> = summaries().first()
        syncManager.syncHistory(latest)
    }

    suspend fun recordReminderSent(date: LocalDate) {
        val key = date.toString()
        ensureDayExists(key)
        dao.incrementSent(key)
        
        // Log event
        eventDao.insert(SedentaryEvent(
            dateIso = key,
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis(),
            type = "REMINDER_SENT"
        ))
        
        syncAll()
    }

    suspend fun recordReminderAcknowledged(date: LocalDate) {
        val key = date.toString()
        ensureDayExists(key)
        dao.incrementAcknowledged(key)
        
        // Find if there was a reminder sent recently and mark as met
        // For simplicity, we just add a GOAL_MET event
        eventDao.insert(SedentaryEvent(
            dateIso = key,
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis(),
            type = "GOAL_MET"
        ))
        
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

        // Manage Sedentary Event
        val activeEvent = eventDao.getActiveEvent()
        val now = System.currentTimeMillis()
        if (activeEvent != null && activeEvent.dateIso == key && activeEvent.type == "SEDENTARY") {
            // Update existing event
            val updatedDuration = activeEvent.durationSeconds + seconds
            eventDao.update(activeEvent.copy(durationSeconds = updatedDuration))
        } else {
            // Start new event
            eventDao.insert(SedentaryEvent(
                dateIso = key,
                startTimeMillis = now - (seconds * 1000),
                endTimeMillis = null,
                type = "SEDENTARY",
                durationSeconds = seconds
            ))
        }

        syncAll()
    }

    suspend fun closeActiveSedentaryEvent() {
        val activeEvent = eventDao.getActiveEvent()
        if (activeEvent != null && activeEvent.type == "SEDENTARY") {
            eventDao.update(activeEvent.copy(endTimeMillis = System.currentTimeMillis()))
        }
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
            val settings = settingsRepository.currentSettings()
            val threshold = settings.sitThresholdUnit.toDuration(settings.sitThresholdValue).seconds.toInt()
            dao.upsert(DailySummary(dateIso, 0, 0, 0, List(24) { 0 }, threshold))
        }
    }

    suspend fun injectMockScenario(scenario: String) {
        val today = LocalDate.now()
        val summaries = mutableListOf<DailySummary>()
        val settings = settingsRepository.currentSettings()
        val currentThreshold = settings.sitThresholdUnit.toDuration(settings.sitThresholdValue).seconds.toInt()
        
        // Generate 14 days of data
        for (i in 0..14) {
            val date = today.minusDays(i.toLong())
            val dateIso = date.toString()
            
            val isLastWeek = i > 7
            val baseSittingSec = 300 * 60
            // Create significant daily variance (between -120 and +120 minutes)
            val jitter = (-120..120).random() * 60
            
            val sittingSec = when (scenario) {
                "BETTER" -> {
                    // Gradual improvement: older days in the week sit more
                    val improvementTrend = (i * 30 * 60) // Older days sit more
                    (200 * 60) + improvementTrend + jitter
                }
                "WORSE" -> {
                    // Gradual decline: newer days sit more
                    val declineTrend = ((14 - i) * 30 * 60)
                    (200 * 60) + declineTrend + jitter
                }
                else -> baseSittingSec + jitter
            }.coerceAtLeast(1800) // Minimum 30 mins sedentary
            
            val hourly = MutableList(24) { 0 }
            // Distribute sitting seconds across the day with much more extreme weights
            val weights = DoubleArray(24) { hour ->
                when (hour) {
                    14 -> 50.0 // Massive Hotspot
                    15 -> 30.0
                    11 -> 25.0
                    in 9..17 -> 5.0 // Office hours
                    else -> 0.0 // Night/Early morning is completely active
                }
            }
            val totalWeight = weights.sum()
            var distributedSec = 0
            for (hour in 0..23) {
                if (weights[hour] == 0.0) {
                    hourly[hour] = 0
                    continue
                }
                val share = ((weights[hour] / totalWeight) * sittingSec).toInt().coerceAtMost(3600)
                hourly[hour] = share
                distributedSec += share
            }
            
            // Adjust any rounding discrepancy to the hotspot
            if (sittingSec > distributedSec) {
                hourly[14] = (hourly[14] + (sittingSec - distributedSec)).coerceAtMost(3600)
            }
            
            summaries.add(DailySummary(
                dateIso = dateIso,
                sedentarySeconds = sittingSec,
                remindersSent = 10,
                remindersAcknowledged = if (scenario == "BETTER") 8 else 3,
                hourlySedentarySeconds = hourly,
                sedentaryThresholdSeconds = currentThreshold
            ))
        }
        
        summaries.forEach { dao.upsert(it) }
        
        // Mock Sedentary Events for the last 14 days
        eventDao.deleteForDay(today.toString())
        for (i in 0..14) {
            val date = today.minusDays(i.toLong())
            val dateIso = date.toString()
            eventDao.deleteForDay(dateIso)
            
            val nowBase = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Morning Session (9:00 - 10:30) - 90m sedentary (BREACH)
            eventDao.insert(SedentaryEvent(dateIso = dateIso, startTimeMillis = nowBase, endTimeMillis = nowBase + 5400000, type = "SEDENTARY", durationSeconds = 5400))
            eventDao.insert(SedentaryEvent(dateIso = dateIso, startTimeMillis = nowBase + (currentThreshold * 1000L), endTimeMillis = nowBase + (currentThreshold * 1000L), type = "REMINDER_SENT"))
            eventDao.insert(SedentaryEvent(dateIso = dateIso, startTimeMillis = nowBase + 5400000, endTimeMillis = nowBase + 5400000, type = "GOAL_MET"))

            // Lunch Break (12:00 - 13:00) - Active (No events)

            // Afternoon Slump (14:00 - 15:00) - 60m sedentary (BREACH, IGNORED)
            val afternoonBase = nowBase + 18000000 // +5 hours
            eventDao.insert(SedentaryEvent(dateIso = dateIso, startTimeMillis = afternoonBase, endTimeMillis = afternoonBase + 3600000, type = "SEDENTARY", durationSeconds = 3600))
            eventDao.insert(SedentaryEvent(dateIso = dateIso, startTimeMillis = afternoonBase + (currentThreshold * 1000L), endTimeMillis = afternoonBase + (currentThreshold * 1000L), type = "REMINDER_SENT"))
            
            // Short evening sitting (17:00 - 17:15) - 15m (SAFE)
            val eveningBase = nowBase + 28800000 // +8 hours
            eventDao.insert(SedentaryEvent(dateIso = dateIso, startTimeMillis = eveningBase, endTimeMillis = eveningBase + 900000, type = "SEDENTARY", durationSeconds = 900))
        }

        // Mock Personal Bests
        pbDao.upsert(PersonalBest("max_streak", 12, today.minusDays(5).toString()))
        pbDao.upsert(PersonalBest("min_sedentary", 7200, today.minusDays(10).toString()))
        pbDao.upsert(PersonalBest("max_response_rate", 100, today.minusDays(2).toString()))
        
        syncAll()
    }
}
