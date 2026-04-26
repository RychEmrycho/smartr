package com.smartr.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.smartr.presentation.theme.LevelGold

@Composable
fun VitalityRing(
    wellnessScore: Int,
    xpProgress: Float,
    level: Int,
    scoreColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer Ring: XP Progress
        CircularProgressIndicator(
            progress = { xpProgress },
            modifier = Modifier.fillMaxSize(),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = LevelGold,
                trackColor = LevelGold.copy(alpha = 0.1f)
            ),
            strokeWidth = 6.dp
        )

        // Inner Ring: Today's Wellness
        CircularProgressIndicator(
            progress = { wellnessScore / 100f },
            modifier = Modifier.fillMaxSize().padding(10.dp),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = scoreColor,
                trackColor = scoreColor.copy(alpha = 0.1f)
            ),
            strokeWidth = 6.dp
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$level",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = LevelGold
            )
            Text(
                text = "LVL",
                style = MaterialTheme.typography.labelSmall,
                color = LevelGold.copy(alpha = 0.7f)
            )
        }
    }
}
