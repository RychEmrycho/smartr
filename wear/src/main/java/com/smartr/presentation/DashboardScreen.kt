package com.smartr.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.smartr.R
import com.smartr.Screen
import com.smartr.presentation.component.Sparkline

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = viewModel()
) {
    val insights by viewModel.insights.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") {
                ListHeader { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium) }
            }

            item(key = "mark_done") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.markAsDone()
                        Toast.makeText(context, context.getString(R.string.dashboard_mark_done), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(R.string.dashboard_mark_done))
                }
            }

            item(key = "sync") {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.triggerManualSync()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.dashboard_sync_now))
                    }
                }
            }

            item(key = "wellness_score") {
                val scoreColor = getWellnessColor(insights.wellnessScore)

                TitleCard(
                    onClick = { },
                    title = { Text(stringResource(R.string.dashboard_wellness_score)) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleColor = scoreColor
                    ),
                    subtitle = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { insights.wellnessScore / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    colors = ProgressIndicatorDefaults.colors(
                                        indicatorColor = scoreColor,
                                        trackColor = scoreColor.copy(alpha = 0.2f)
                                    ),
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "${insights.wellnessScore}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dashboard_streak_format, insights.currentStreak),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.dashboard_wellness_positive), style = MaterialTheme.typography.labelSmall)
                }
            }

            item(key = "trends") {
                val scoreColor = getWellnessColor(insights.wellnessScore)

                AppCard(
                    onClick = { navController.navigate(Screen.History.route) },
                    appName = { Text(stringResource(R.string.dashboard_activity_insights)) },
                    appImage = { 
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = scoreColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    title = { Text(stringResource(R.string.dashboard_7day_trend)) },
                    time = { Text("Last 7d") },
                ) {
                    Column {
                        Text(
                            stringResource(R.string.dashboard_avg_sedentary_format, insights.averageSedentaryMinutes),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        if (trendData.size > 1) {
                            Sparkline(
                                data = trendData,
                                color = scoreColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .padding(vertical = 4.dp)
                            )
                        } else {
                            Text(
                                stringResource(R.string.dashboard_collect_data),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "history_btn") {
                Button(
                    onClick = { navController.navigate(Screen.History.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(stringResource(R.string.dashboard_view_history))
                }
            }

            item(key = "settings_btn") {
                Button(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(stringResource(R.string.dashboard_settings))
                }
            }
        }
    }
}

@Composable
private fun getWellnessColor(score: Int): androidx.compose.ui.graphics.Color {
    return when {
        score > 80 -> colorResource(R.color.wellness_high)
        score > 50 -> colorResource(R.color.wellness_mid)
        else -> colorResource(R.color.wellness_low)
    }
}
