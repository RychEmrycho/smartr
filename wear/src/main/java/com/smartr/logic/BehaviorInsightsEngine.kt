package com.smartr.logic

import com.smartr.data.history.DailySummary

data class InsightSnapshot(
    val averageSedentaryMinutes: Int,
    val totalReminders: Int,
    val reminderResponseRate: Int,
    val wellnessScore: Int,
    val currentStreak: Int,
    val level: Int,
    val xpProgress: Float, // 0.0 to 1.0 for current level
    val rank: String,
    val totalXp: Int
)

class BehaviorInsightsEngine {
    fun build(summaries: List<DailySummary>): InsightSnapshot {
        if (summaries.isEmpty()) return InsightSnapshot(0, 0, 100, 100, 0, 1, 0f, "Novice", 0)
        
        val avgSedentary = summaries.map { it.sedentaryMinutes }.average().toInt()
        val reminders = summaries.sumOf { it.remindersSent }
        val acknowledged = summaries.sumOf { it.remindersAcknowledged }
        val responseRate = if (reminders == 0) 100 else ((acknowledged * 100.0) / reminders).toInt()

        // Wellness Score (0-100)
        val sedentaryPenalty = (avgSedentary / 10.0).coerceAtMost(50.0)
        val responseBonus = (responseRate / 2.0).coerceAtMost(50.0)
        val score = (100 - sedentaryPenalty + responseBonus).toInt().coerceIn(0, 100)

        // Streak: consecutive days with sedentaryMinutes below 300
        var streak = 0
        val sortedSummaries = summaries.sortedByDescending { it.dateIso }
        for (summary in sortedSummaries) {
            if (summary.sedentaryMinutes < 300) {
                streak++
            } else {
                break
            }
        }

        // Gamification: XP and Levels
        // 50 XP per acknowledgment, -1 XP per 10 mins sitting
        val xpFromAcknowledged = acknowledged * 50
        val xpFromSitting = (summaries.sumOf { it.sedentaryMinutes } / 10) * -1
        val totalXp = (xpFromAcknowledged + xpFromSitting).coerceAtMost(Int.MAX_VALUE).coerceAtLeast(0)

        // Simple level logic: each level requires 500 XP more than previous
        // Lvl 1: 0, Lvl 2: 500, Lvl 3: 1500, Lvl 4: 3000... (Triangular numbers * 500)
        // Roughly: totalXp = 250 * L * (L-1)
        // Solving for L: L = (1 + sqrt(1 + 8*totalXp/250)) / 2
        val level = ((1 + Math.sqrt(1.0 + 8.0 * totalXp / 500.0)) / 2.0).toInt().coerceAtLeast(1)
        
        val xpForCurrentLevel = 250 * level * (level - 1)
        val xpForNextLevel = 250 * (level + 1) * level
        val xpProgress = if (xpForNextLevel == xpForCurrentLevel) 0f 
                         else ((totalXp - xpForCurrentLevel).toFloat() / (xpForNextLevel - xpForCurrentLevel))

        val rank = when {
            level >= 20 -> "Zen Master"
            level >= 10 -> "Flow Master"
            level >= 5 -> "Active"
            else -> "Novice"
        }

        return InsightSnapshot(
            averageSedentaryMinutes = avgSedentary,
            totalReminders = reminders,
            reminderResponseRate = responseRate,
            wellnessScore = score,
            currentStreak = streak,
            level = level,
            xpProgress = xpProgress,
            rank = rank,
            totalXp = totalXp
        )
    }
}
