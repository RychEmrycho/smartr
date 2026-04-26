package com.smartr.data.history

import android.content.Context
import com.smartr.data.WearSyncManager
import com.smartr.data.SettingsRepository
import com.smartr.data.TrackingStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalTime
import java.time.Instant
import java.time.Duration
import java.util.UUID

class HistoryRepository(private val context: Context) {
    private val database = HistoryDatabase.get(context)
    private val dao = database.dailySummaryDao()
    private val pbDao = database.personalBestDao()
    private val eventDao = database.eventDao()
    private val syncManager = WearSyncManager(context)
    private val settingsRepository = SettingsRepository(context)
    private val trackingRepository = TrackingStateRepository(context)

    fun summaries(): Flow<List<DailySummary>> = dao.latest30Days()

    fun personalBests(): Flow<List<PersonalBest>> = pbDao.getAll()

    fun eventsForDay(date: LocalDate): Flow<List<Event>> {
        val start = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toString()
        val end = date.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toString()
        return eventDao.eventsForRange(start, end)
    }

    private suspend fun syncAll() {
        val latest: List<DailySummary> = summaries().first()
        syncManager.syncHistory(latest)
    }

    suspend fun recordReminderSent(date: LocalDate, durationSeconds: Int) {
        val key = date.toString()
        ensureDayExists(key)
        dao.incrementSent(key)
        
        val sessionId = trackingRepository.activeSessionId.first()

        // Log event
        eventDao.insert(Event(
            timestamp = Instant.now().toString(),
            type = EventType.REMINDER_SENT,
            sessionId = sessionId,
            metadata = mapOf("duration" to durationSeconds.toString())
        ))
        
        syncAll()
    }

    suspend fun recordReminderAcknowledged(date: LocalDate) {
        val key = date.toString()
        ensureDayExists(key)
        dao.incrementAcknowledged(key)
        
        // Mark the active sedentary event as stopped
        closeActiveSedentaryEvent("Manual reset")
        
        syncAll()
    }

    suspend fun addSedentaryMinutesSample(date: LocalDate, seconds: Int) {
        if (seconds <= 0) return
        val key = date.toString()
        ensureDayExists(key)
        
        // Update total summary
        dao.addSeconds(key, seconds)
        
        // Update hourly summary
        val currentSummary = dao.findByDate(key)
        currentSummary?.let { summary ->
            val hour = LocalTime.now().hour
            val updatedHourly = summary.hourlySedentarySeconds.toMutableList()
            updatedHourly[hour] = (updatedHourly[hour] + seconds).coerceAtMost(3600)
            dao.upsert(summary.copy(hourlySedentarySeconds = updatedHourly))
        }

        // Manage Sedentary Event
        val activeSessionId = trackingRepository.activeSessionId.first()
        
        if (activeSessionId == null) {
            // Start a new sedentary session
            val newSessionId = UUID.randomUUID().toString()
            trackingRepository.setActiveSessionId(newSessionId)
            eventDao.insert(Event(
                timestamp = Instant.now().minusSeconds(seconds.toLong()).toString(),
                type = EventType.SEDENTARY_START,
                sessionId = newSessionId,
                metadata = null
            ))
        } else {
            // Metadata for START events could be updated if needed, 
            // but usually we just log START and then STOP with duration.
        }

        syncAll()
    }

    suspend fun closeActiveSedentaryEvent(reason: String = "Movement") {
        val sessionId = trackingRepository.activeSessionId.first()
        if (sessionId != null) {
            // Find when this session started to calculate final duration
            val startEvent = eventDao.getLatestSessionEvent(EventType.SEDENTARY_START)
            val duration = if (startEvent != null && startEvent.sessionId == sessionId) {
                Duration.between(Instant.parse(startEvent.timestamp), Instant.now()).toSeconds().toInt()
            } else 0

            eventDao.insert(Event(
                timestamp = Instant.now().toString(),
                type = EventType.SEDENTARY_STOPPED,
                sessionId = sessionId,
                metadata = mapOf(
                    "duration" to duration.toString(),
                    "reason" to reason
                )
            ))
            trackingRepository.setActiveSessionId(null)
        }
    }

    suspend fun reconcileInterruptedEvents() {
        val sessionId = trackingRepository.activeSessionId.first()
        if (sessionId != null) {
            // Found a hanging session. Mark it as RESET.
            eventDao.insert(Event(
                timestamp = Instant.now().toString(),
                type = EventType.SEDENTARY_RESET,
                sessionId = sessionId,
                metadata = mapOf("reason" to "Device restart")
            ))
            trackingRepository.setActiveSessionId(null)
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
        eventDao.clearAll()
        val nowMillis = System.currentTimeMillis()
        
        for (i in 0..14) {
            val date = today.minusDays(i.toLong())
            
            val morningStart = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant()
            val morningEnd = morningStart.plusSeconds(5400) // 90m
            val session1Id = UUID.randomUUID().toString()
            
            if (morningStart.toEpochMilli() < nowMillis) {
                eventDao.insert(Event(timestamp = morningStart.toString(), type = EventType.SEDENTARY_START, sessionId = session1Id, metadata = null))
                if (morningStart.plusSeconds(currentThreshold.toLong()).toEpochMilli() < nowMillis) {
                    eventDao.insert(Event(
                        timestamp = morningStart.plusSeconds(currentThreshold.toLong()).toString(),
                        type = EventType.REMINDER_SENT,
                        sessionId = session1Id,
                        metadata = mapOf("duration" to currentThreshold.toString())
                    ))
                }
                if (morningEnd.toEpochMilli() < nowMillis) {
                    eventDao.insert(Event(
                        timestamp = morningEnd.toString(),
                        type = EventType.SEDENTARY_STOPPED,
                        sessionId = session1Id,
                        metadata = mapOf("duration" to "5400", "reason" to "Movement")
                    ))
                }
            }

            val afternoonStart = morningStart.plusSeconds(18000) // 14:00
            val afternoonEnd = afternoonStart.plusSeconds(3600) // 60m
            val session2Id = UUID.randomUUID().toString()
            
            if (afternoonStart.toEpochMilli() < nowMillis) {
                eventDao.insert(Event(timestamp = afternoonStart.toString(), type = EventType.SEDENTARY_START, sessionId = session2Id, metadata = null))
                if (afternoonStart.plusSeconds(currentThreshold.toLong()).toEpochMilli() < nowMillis) {
                    eventDao.insert(Event(
                        timestamp = afternoonStart.plusSeconds(currentThreshold.toLong()).toString(),
                        type = EventType.REMINDER_SENT,
                        sessionId = session2Id,
                        metadata = mapOf("duration" to currentThreshold.toString())
                    ))
                }
                if (afternoonEnd.toEpochMilli() < nowMillis) {
                    eventDao.insert(Event(
                        timestamp = afternoonEnd.toString(),
                        type = EventType.SEDENTARY_STOPPED,
                        sessionId = session2Id,
                        metadata = mapOf("duration" to "3600", "reason" to "Movement")
                    ))
                }
            }
            
            val eveningStart = morningStart.plusSeconds(28800) // 17:00
            val eveningEnd = eveningStart.plusSeconds(900) // 15m
            val session3Id = UUID.randomUUID().toString()
            
            if (eveningStart.toEpochMilli() < nowMillis) {
                eventDao.insert(Event(timestamp = eveningStart.toString(), type = EventType.SEDENTARY_START, sessionId = session3Id, metadata = null))
                if (eveningEnd.toEpochMilli() < nowMillis) {
                    eventDao.insert(Event(
                        timestamp = eveningEnd.toString(),
                        type = EventType.SEDENTARY_STOPPED,
                        sessionId = session3Id,
                        metadata = mapOf("duration" to "900", "reason" to "Movement")
                    ))
                }
            }
        }

        // Mock Personal Bests
        pbDao.upsert(PersonalBest("max_streak", 12, today.minusDays(5).toString()))
        pbDao.upsert(PersonalBest("min_sedentary", 7200, today.minusDays(10).toString()))
        pbDao.upsert(PersonalBest("max_response_rate", 100, today.minusDays(2).toString()))
        
        syncAll()
    }
}
