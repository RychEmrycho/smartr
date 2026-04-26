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
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Weekend
import com.smartr.data.history.EventType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DailyDetailScreen(
    dateIso: String,
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel()
) {
    val summaries by viewModel.summaries.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val events by viewModel.getEvents(dateIso).collectAsState(initial = emptyList())
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

                if (events.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text("No activity events logged", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    items(events.size) { index ->
                        val event = events[index]
                        val nextEvent = if (index < events.size - 1) events[index + 1] else null
                        
                        // Show Time Section Header if period changed (descending order)
                        val currentHour = Instant.parse(event.timestamp).atZone(ZoneId.systemDefault()).hour
                        val nextHour = nextEvent?.let { Instant.parse(it.timestamp).atZone(ZoneId.systemDefault()).hour } ?: -1
                        
                        val currentSection = getTimeSection(currentHour)
                        val nextSection = if (nextHour != -1) getTimeSection(nextHour) else null
                        
                        if (index == 0 || currentSection != nextSection) {
                            SectionHeader(stringResource(currentSection))
                        }

                        EventTimelineItem(
                            event = event,
                            thresholdSeconds = summary.sedentaryThresholdSeconds,
                            isLast = index == events.size - 1
                        )
                    }
                }
            }
        }
    }
}

private fun getTimeSection(hour: Int): Int {
    return when (hour) {
        in 0..11 -> R.string.detail_morning
        in 12..16 -> R.string.detail_afternoon
        else -> R.string.detail_evening
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
private fun EventTimelineItem(
    event: com.smartr.data.history.Event,
    thresholdSeconds: Int,
    isLast: Boolean
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    val startTime = Instant.parse(event.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    
    val (icon, color, title, subtitle) = when (event.type) {
        EventType.SEDENTARY_START -> {
            Quad(
                Icons.Default.Weekend, 
                WellnessMid, 
                "Sedentary detected", 
                "Session started"
            )
        }
        EventType.SEDENTARY_STOPPED -> {
            val durationSeconds = event.metadata?.get("duration")?.toIntOrNull() ?: 0
            val reason = event.metadata?.get("reason") ?: "Movement"
            val isBreach = durationSeconds > thresholdSeconds
            val color = if (isBreach) WellnessLow else WellnessHigh
            
            Quad(
                Icons.AutoMirrored.Filled.DirectionsRun, 
                color, 
                "Sedentary stopped", 
                "${DurationFormatter.format(context, durationSeconds)} ($reason)"
            )
        }
        EventType.SEDENTARY_RESET -> {
            Quad(
                Icons.Default.Refresh, 
                MaterialTheme.colorScheme.error, 
                "Sedentary reset", 
                event.metadata?.get("reason") ?: "Interrupted"
            )
        }
        EventType.REMINDER_SENT -> {
            val durationSeconds = event.metadata?.get("duration")?.toIntOrNull() ?: 0
            Quad(
                Icons.Default.Notifications, 
                MaterialTheme.colorScheme.primary, 
                "Reminder sent", 
                "${DurationFormatter.format(context, durationSeconds)} sedentary"
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(IntrinsicSize.Min),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
                    drawLine(
                        color = lineColor,
                        start = center.copy(y = 24.dp.toPx()),
                        end = center.copy(y = size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = startTime.format(timeFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
