package com.smartr.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import com.smartr.presentation.theme.WellnessLow
import com.smartr.presentation.theme.WellnessMid
import com.smartr.presentation.theme.WellnessHigh

@Composable
fun HourlyHeatmap(
    hourlyData: List<Int>,
    thresholdSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hourlyData.forEach { seconds ->
                val heightPercent = (seconds / 3600f).coerceIn(0.1f, 1f)
                val criticality = com.smartr.logic.SedentaryCriticality.fromDuration(seconds, thresholdSeconds)
                val color = criticality.getColor()
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightPercent)
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            for (i in 0 until 24) {
                val text = when (i) {
                    0 -> "00"
                    6 -> "06"
                    12 -> "12"
                    18 -> "18"
                    23 -> "23"
                    else -> ""
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 6.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

