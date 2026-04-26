package com.smartr.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.smartr.presentation.theme.WellnessLow
import com.smartr.presentation.theme.WellnessMid
import com.smartr.presentation.theme.WellnessHigh

@Composable
fun HourlyHeatmap(
    hourlyData: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        hourlyData.forEach { minutes ->
            val heightPercent = (minutes / 60f).coerceIn(0.1f, 1f)
            val color = when {
                minutes > 45 -> WellnessLow
                minutes > 20 -> WellnessMid
                else -> WellnessHigh.copy(alpha = 0.5f)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightPercent)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}
