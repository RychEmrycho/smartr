package com.smartr.logic

import android.content.Context
import com.smartr.R

object DurationFormatter {
    fun format(context: Context, totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0s"
        
        if (totalSeconds < 60) {
            return context.getString(R.string.duration_seconds, totalSeconds)
        }
        
        val totalMinutes = totalSeconds / 60
        val remainingSeconds = totalSeconds % 60
        
        if (totalMinutes < 60) {
            return if (remainingSeconds == 0) {
                context.getString(R.string.duration_minutes_only, totalMinutes)
            } else {
                context.getString(R.string.duration_minutes_seconds, totalMinutes, remainingSeconds)
            }
        }
        
        val totalHours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        
        if (totalHours < 24) {
            return if (remainingMinutes == 0) {
                context.getString(R.string.duration_hours_only, totalHours)
            } else {
                context.getString(R.string.duration_hours_minutes, totalHours, remainingMinutes)
            }
        }
        
        val totalDays = totalHours / 24
        val remainingHours = totalHours % 24
        // Since we are formatting for a watch, if minutes are 0 we can show just Days + Hours or just Days.
        return if (remainingHours == 0 && remainingMinutes == 0) {
            context.getString(R.string.duration_days_only, totalDays)
        } else {
            // For simplicity and readability on Wear, we'll keep the full format for days or 
            // you might want a "1d 2h" version too. Here is the requested 1d 2h 3m style.
            context.getString(R.string.duration_days_hours_minutes, totalDays, remainingHours, remainingMinutes)
        }
    }
}
