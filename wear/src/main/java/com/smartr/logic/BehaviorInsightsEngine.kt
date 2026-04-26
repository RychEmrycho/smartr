package com.smartr.logic

import com.smartr.data.history.DailySummary

data class InsightSnapshot(
    val averageSedentarySeconds: Int,
    val totalReminders: Int,
    val reminderResponseRate: Int,
    val wellnessScore: Int,
    val currentStreak: Int,
    val level: Int,
    val xpProgress: Float,
    val rank: String,
    val totalXp: Int,
    val dangerZoneHour: Int? = null,
    val weeklyChangePercent: Int? = null,
    val performanceGrade: String = "B"
)

class BehaviorInsightsEngine {
    fun build(summaries: List<DailySummary>): InsightSnapshot {
        if (summaries.isEmpty()) return InsightSnapshot(0, 0, 100, 100, 0, 1, 0f, "Novice", 0)
        
        val sorted = summaries.sortedByDescending { it.dateIso }
        val avgSedentary = summaries.map { it.sedentarySeconds }.average().toInt()
        val reminders = summaries.sumOf { it.remindersSent }
        val acknowledged = summaries.sumOf { it.remindersAcknowledged }
        val responseRate = if (reminders == 0) 100 else ((acknowledged * 100.0) / reminders).toInt()

        // Wellness & Streak
        val score = calculateWellness(avgSedentary, responseRate)
        val streak = calculateStreak(sorted)

        // XP & Level
        val totalXp = calculateTotalXp(summaries)
        val level = calculateLevel(totalXp)
        val xpProgress = calculateXpProgress(totalXp, level)
        val rank = calculateRank(level)

        // Danger Zone (last 7 days)
        val last7Days = sorted.take(7)
        val dangerZone = calculateDangerZone(last7Days)

        // Weekly Comparison
        val thisWeekAvg = last7Days.map { it.sedentarySeconds }.average()
        val prevWeekAvg = sorted.drop(7).take(7).map { it.sedentarySeconds }.average()
        val changePercent = if (prevWeekAvg > 0) {
            (((thisWeekAvg - prevWeekAvg) / prevWeekAvg) * 100).toInt()
        } else null

        val grade = when {
            score >= 90 -> "A+"
            score >= 80 -> "A"
            score >= 70 -> "B"
            score >= 60 -> "C"
            else -> "D"
        }

        return InsightSnapshot(
            averageSedentarySeconds = avgSedentary,
            totalReminders = reminders,
            reminderResponseRate = responseRate,
            wellnessScore = score,
            currentStreak = streak,
            level = level,
            xpProgress = xpProgress,
            rank = rank,
            totalXp = totalXp,
            dangerZoneHour = dangerZone,
            weeklyChangePercent = changePercent,
            performanceGrade = grade
        )
    }

    private fun calculateWellness(avgSedentarySeconds: Int, responseRate: Int): Int {
        val sedentaryPenalty = (avgSedentarySeconds / 600.0).coerceAtMost(50.0) // 10 mins = 1 penalty point
        val responseBonus = (responseRate / 2.0).coerceAtMost(50.0)
        return (100 - sedentaryPenalty + responseBonus).toInt().coerceIn(0, 100)
    }

    private fun calculateStreak(sorted: List<DailySummary>): Int {
        var streak = 0
        for (summary in sorted) {
            if (summary.sedentarySeconds < 300 * 60) streak++ else break
        }
        return streak
    }

    private fun calculateTotalXp(summaries: List<DailySummary>): Int {
        val acknowledged = summaries.sumOf { it.remindersAcknowledged }
        val xpFromAcknowledged = acknowledged * 50
        val xpFromSitting = (summaries.sumOf { it.sedentarySeconds } / 600) * -1
        return (xpFromAcknowledged + xpFromSitting).coerceAtLeast(0)
    }

    private fun calculateLevel(totalXp: Int): Int {
        return ((1 + Math.sqrt(1.0 + 8.0 * totalXp / 500.0)) / 2.0).toInt().coerceAtLeast(1)
    }

    private fun calculateXpProgress(totalXp: Int, level: Int): Float {
        val xpForCurrentLevel = 250 * level * (level - 1)
        val xpForNextLevel = 250 * (level + 1) * level
        return if (xpForNextLevel == xpForCurrentLevel) 0f 
               else ((totalXp - xpForCurrentLevel).toFloat() / (xpForNextLevel - xpForCurrentLevel))
    }

    private fun calculateRank(level: Int) = when {
        level >= 20 -> "Zen Master"
        level >= 10 -> "Flow Master"
        level >= 5 -> "Active"
        else -> "Novice"
    }

    private fun calculateDangerZone(summaries: List<DailySummary>): Int? {
        if (summaries.isEmpty()) return null
        val hourlyTotals = IntArray(24)
        summaries.forEach { summary ->
            summary.hourlySedentarySeconds.forEachIndexed { index, secs ->
                hourlyTotals[index] += secs
            }
        }
        val maxSecs = hourlyTotals.maxOrNull() ?: 0
        return if (maxSecs > 0) hourlyTotals.indexOf(maxSecs) else null
    }
}
