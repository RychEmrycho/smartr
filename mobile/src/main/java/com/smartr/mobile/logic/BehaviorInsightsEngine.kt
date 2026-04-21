package com.smartr.mobile.logic

import com.smartr.mobile.data.history.DailySummaryEntity

data class InsightSnapshot(
    val averageSedentaryMinutes: Int,
    val totalReminders: Int,
    val reminderResponseRate: Int,
    val wellnessScore: Int,
    val currentStreak: Int
)

class BehaviorInsightsEngine {
    fun build(summaries: List<DailySummaryEntity>): InsightSnapshot {
        if (summaries.isEmpty()) return InsightSnapshot(0, 0, 100, 100, 0)
        
        val avgSedentary = summaries.map { it.sedentaryMinutes }.average().toInt()
        val reminders = summaries.sumOf { it.remindersSent }
        val acknowledged = summaries.sumOf { it.remindersAcknowledged }
        val responseRate = if (reminders == 0) 100 else ((acknowledged * 100.0) / reminders).toInt()

        // Premium logic: Wellness Score (0-100)
        val sedentaryPenalty = (avgSedentary / 10.0).coerceAtMost(50.0)
        val responseBonus = (responseRate / 2.0).coerceAtMost(50.0)
        val score = (100 - sedentaryPenalty + responseBonus).toInt().coerceIn(0, 100)

        // Streak: consecutive days with sedentaryMinutes below a threshold (e.g., 300 mins)
        var streak = 0
        val sortedSummaries = summaries.sortedByDescending { it.dateIso }
        for (summary in sortedSummaries) {
            if (summary.sedentaryMinutes < 300) {
                streak++
            } else {
                break
            }
        }

        return InsightSnapshot(avgSedentary, reminders, responseRate, score, streak)
    }
}
