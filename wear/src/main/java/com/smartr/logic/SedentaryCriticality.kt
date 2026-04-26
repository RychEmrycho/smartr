package com.smartr.logic

import androidx.compose.ui.graphics.Color
import com.smartr.presentation.theme.WellnessHigh
import com.smartr.presentation.theme.WellnessLow
import com.smartr.presentation.theme.WellnessMid

enum class SedentaryCriticality(val emoji: String) {
    LOW("🙂"),
    MEDIUM("⚠️"),
    HIGH("🚨");

    fun getColor(): Color = when (this) {
        LOW -> WellnessHigh.copy(alpha = 0.5f)
        MEDIUM -> WellnessMid
        HIGH -> WellnessLow
    }

    companion object {
        fun fromDuration(seconds: Int, thresholdSeconds: Int): SedentaryCriticality {
            return when {
                seconds >= thresholdSeconds -> HIGH
                seconds >= thresholdSeconds / 2 -> MEDIUM
                else -> LOW
            }
        }
    }
}
