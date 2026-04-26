package com.smartr.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.smartr.R
import com.smartr.logic.DurationFormatter
import com.smartr.presentation.theme.WellnessHigh
import com.smartr.presentation.theme.WellnessLow
import com.smartr.presentation.theme.WellnessMid
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalContext

@Composable
fun DailyDetailScreen(
    dateIso: String,
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel()
) {
    val summaries by viewModel.summaries.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val summary = remember(summaries, dateIso) {
        summaries.find { it.dateIso == dateIso }
    }
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    val date = try {
        val parsed = LocalDate.parse(dateIso)
        parsed.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault()))
    } catch (e: Exception) {
        dateIso
    }

    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        if (summary == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty))
            }
        } else {
            val complianceRate = if (summary.remindersSent == 0) 100 
                else ((summary.remindersAcknowledged * 100.0) / summary.remindersSent).toInt()

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                item {
                    ListHeader {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(date, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.history_item_compliance_format, summary.remindersSent, complianceRate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    val historicalThreshold = summary.sedentaryThresholdSeconds
                    val currentThreshold = settings.sitThresholdValue * 60
                    
                    if (historicalThreshold != currentThreshold) {
                        Card(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.detail_historical_goal, 
                                    DurationFormatter.format(context, historicalThreshold)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    val threshold = summary.sedentaryThresholdSeconds
                    val highThreshold = threshold
                    val midThreshold = threshold / 2

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendItem(
                                WellnessLow, 
                                stringResource(R.string.detail_legend_high_dynamic, DurationFormatter.format(context, highThreshold))
                            )
                            LegendItem(
                                WellnessMid, 
                                stringResource(R.string.detail_legend_mid_dynamic, DurationFormatter.format(context, midThreshold))
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        LegendItem(WellnessHigh.copy(alpha = 0.5f), stringResource(R.string.detail_legend_low))
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.detail_morning))
                }

                items(12) { hour ->
                    HourlyTimelineItem(
                        hour, 
                        summary.hourlySedentarySeconds.getOrElse(hour) { 0 }, 
                        summary.sedentaryThresholdSeconds,
                        isLast = false
                    )
                }

                item {
                    SectionHeader(stringResource(R.string.detail_afternoon))
                }

                items(5) { i ->
                    val hour = i + 12
                    HourlyTimelineItem(
                        hour, 
                        summary.hourlySedentarySeconds.getOrElse(hour) { 0 }, 
                        summary.sedentaryThresholdSeconds,
                        isLast = false
                    )
                }

                item {
                    SectionHeader(stringResource(R.string.detail_evening))
                }

                items(7) { i ->
                    val hour = i + 17
                    HourlyTimelineItem(
                        hour, 
                        summary.hourlySedentarySeconds.getOrElse(hour) { 0 }, 
                        summary.sedentaryThresholdSeconds,
                        isLast = hour == 23
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 8.dp)
    )
}

@Composable
private fun HourlyTimelineItem(hour: Int, sedentarySeconds: Int, thresholdSeconds: Int, isLast: Boolean) {
    val context = LocalContext.current
    val color = when {
        sedentarySeconds > thresholdSeconds -> WellnessLow
        sedentarySeconds > thresholdSeconds / 2 -> WellnessMid
        else -> WellnessHigh.copy(alpha = 0.5f)
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Vertical Line
            if (!isLast) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = lineColor,
                        start = center.copy(y = 12.dp.toPx()),
                        end = center.copy(y = size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            
            // Dot
            Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = center.copy(y = 12.dp.toPx())
                )
                if (sedentarySeconds > thresholdSeconds) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = center.copy(y = 12.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = String.format(Locale.getDefault(), "%02d:00", hour),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.detail_sedentary_format, DurationFormatter.format(context, sedentarySeconds)),
                style = MaterialTheme.typography.labelSmall,
                color = if (sedentarySeconds > thresholdSeconds) WellnessLow else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
