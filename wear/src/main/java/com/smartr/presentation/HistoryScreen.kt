package com.smartr.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*

import com.smartr.R
import androidx.compose.ui.res.stringResource

import com.smartr.presentation.component.HourlyHeatmap
import com.smartr.presentation.component.PersonalBestRow
import com.smartr.presentation.theme.LevelGold
import com.smartr.presentation.theme.WellnessHigh
import com.smartr.presentation.theme.WellnessLow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.wear.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.CardDefaults

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val summaries by viewModel.summaries.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val personalBests by viewModel.personalBests.collectAsState()
    val listState = rememberScalingLazyListState()
    
    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") { 
                ListHeader { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium) } 
            }

            // Weekly Insight Card
            item(key = "weekly_insight") {
                TitleCard(
                    onClick = { },
                    title = { Text(stringResource(R.string.history_performance_grade, insights.performanceGrade)) },
                    subtitle = {
                        Column {
                            insights.dangerZoneHour?.let { hour ->
                                Text(
                                    stringResource(R.string.history_danger_zone, hour),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WellnessLow
                                )
                            }
                            insights.weeklyChangePercent?.let { change ->
                                val color = if (change < 0) WellnessHigh else WellnessLow
                                val text = if (change < 0) 
                                    stringResource(R.string.history_weekly_better, Math.abs(change))
                                    else stringResource(R.string.history_weekly_worse, change)
                                Text(text, style = MaterialTheme.typography.labelSmall, color = color)
                            }
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                )
            }

            // Hall of Fame
            if (personalBests.isNotEmpty()) {
                item(key = "pbs") {
                    PersonalBestRow(records = personalBests, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            if (summaries.isEmpty()) {
                item(key = "empty") { 
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium) 
                }
            }

            items(
                items = summaries.reversed().take(10),
                key = { it.dateIso }
            ) { summary ->
                TitleCard(
                    onClick = { },
                    title = { Text(summary.dateIso) },
                    subtitle = {
                        Column {
                            Text(stringResource(R.string.history_item_sitting_format, summary.sedentaryMinutes))
                            Spacer(Modifier.height(4.dp))
                            HourlyHeatmap(
                                hourlyData = summary.hourlySedentary,
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.history_item_reminders_format, summary.remindersSent), 
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            item(key = "footer") {
                Text(
                    stringResource(R.string.history_footer),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
