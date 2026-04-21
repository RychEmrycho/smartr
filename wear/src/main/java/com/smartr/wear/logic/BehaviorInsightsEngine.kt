package com.smartr.wear.logic

import com.smartr.wear.data.history.DailySummary

data class InsightSnapshot(
    val averageSedentaryMinutes: Int,
    val totalReminders: Int,
    val reminderResponseRate: Int
)

class BehaviorInsightsEngine {
    fun build(summaries: List<DailySummary>): InsightSnapshot {
        if (summaries.isEmpty()) return InsightSnapshot(0, 0, 0)
        val avgSedentary = summaries.map { it.sedentaryMinutes }.average().toInt()
        val reminders = summaries.sumOf { it.remindersSent }
        val acknowledged = summaries.sumOf { it.remindersAcknowledged }
        val responseRate = if (reminders == 0) 0 else ((acknowledged * 100.0) / reminders).toInt()
        return InsightSnapshot(avgSedentary, reminders, responseRate)
    }
}
