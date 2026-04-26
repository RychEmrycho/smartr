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
import com.smartr.presentation.component.VitalityRing
import com.smartr.presentation.theme.*

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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = insights.rank.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = getRankColor(insights.rank),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.dashboard_streak_format, insights.currentStreak),
                        style = MaterialTheme.typography.labelSmall,
                        color = WellnessMid
                    )
                }
            }

            item(key = "mark_done") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.markAsDone()
                        Toast.makeText(context, context.getString(R.string.dashboard_mark_done), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(R.string.dashboard_mark_done))
                }
            }

            item(key = "vitality_score") {
                val scoreColor = getWellnessColor(insights.wellnessScore)

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VitalityRing(
                        wellnessScore = insights.wellnessScore,
                        xpProgress = insights.xpProgress,
                        level = insights.level,
                        scoreColor = scoreColor,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_vitality_score),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.dashboard_xp_format, insights.totalXp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            item(key = "help_guide_btn") {
                Button(
                    onClick = { navController.navigate(Screen.VitalityInfo.route) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(stringResource(R.string.dashboard_help_guide))
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
        score > 80 -> WellnessHigh
        score > 50 -> WellnessMid
        else -> WellnessLow
    }
}

@Composable
private fun getRankColor(rank: String): androidx.compose.ui.graphics.Color {
    return when (rank) {
        "Zen Master" -> RankZenMaster
        "Flow Master" -> RankFlowMaster
        "Active" -> RankActive
        else -> RankNovice
    }
}
