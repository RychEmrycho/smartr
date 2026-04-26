package com.smartr.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.smartr.data.history.PersonalBest
import com.smartr.presentation.theme.LevelGold

import androidx.compose.ui.platform.LocalContext
import com.smartr.logic.DurationFormatter

@Composable
fun PersonalBestRow(
    records: List<PersonalBest>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "HALL OF FAME",
            style = MaterialTheme.typography.labelSmall,
            color = LevelGold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            records.take(3).forEach { record ->
                RecordItem(record, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecordItem(record: PersonalBest, modifier: Modifier = Modifier) {
    val label = when (record.recordType) {
        "max_streak" -> "Streak"
        "min_sedentary" -> "Min Sit"
        "max_response_rate" -> "Respond"
        else -> record.recordType
    }
    
    val valueStr = when (record.recordType) {
        "max_response_rate" -> "${record.value}%"
        "min_sedentary" -> DurationFormatter.format(LocalContext.current, record.value)
        "max_streak" -> "${record.value}d"
        else -> "${record.value}"
    }

    AppCard(
        onClick = { },
        appName = { Text(label, style = MaterialTheme.typography.labelSmall) },
        title = { 
            Text(
                valueStr,
                style = MaterialTheme.typography.labelMedium,
                color = LevelGold,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = modifier
    ) {
        // Content not needed for small record items
    }
}
