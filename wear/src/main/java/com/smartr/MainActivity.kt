package com.smartr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smartr.data.AppSettings
import com.smartr.data.SettingsRepository
import com.smartr.data.ThemePreference
import com.smartr.data.TimeIntervalUnit
import com.smartr.data.history.DailySummary
import com.smartr.data.history.HistoryRepository
import com.smartr.logic.BehaviorInsightsEngine
import com.smartr.worker.PassiveRegistrationWorker
import com.smartr.complication.ComplicationUpdater
import com.smartr.logic.PassiveRuntimeStore
import android.widget.Toast
import java.time.LocalDate
import com.smartr.presentation.component.Sparkline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Sync
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import java.time.LocalTime

enum class SettingType {
    NONE, SIT_LIMIT, REMINDER_REPEAT, QUIET_START, QUIET_END, THEME
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(applicationContext)
        val historyRepository = HistoryRepository(applicationContext)

        lifecycleScope.launch {
            settingsRepository.ensureDefaults()
        }
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "passive_registration",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<PassiveRegistrationWorker>().build()
        )

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = SettingsRepository.DEFAULTS)
            val summaries by historyRepository.summaries().collectAsState(initial = emptyList())
            val navController = rememberSwipeDismissableNavController()
            
            val context = LocalContext.current
            val colorScheme = when (settings.theme) {
                ThemePreference.DARK -> ColorScheme()
                ThemePreference.LIGHT -> ColorScheme() // Wear OS is dark-only by design, but we can use default ColorScheme()
                ThemePreference.FOLLOW_SYSTEM -> dynamicColorScheme(context) ?: ColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                AppScaffold {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(summaries, navController, historyRepository) { triggerManualSync() }
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(summaries)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(settings, settingsRepository)
                        }
                    }
                }
            }
        }
    }

    private fun triggerManualSync() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val nodes = com.google.android.gms.wearable.Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                for (node in nodes) {
                    com.google.android.gms.wearable.Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, "/ping", "ping_from_watch".toByteArray())
                        .await()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to trigger sync", e)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    summaries: List<DailySummary>,
    navController: NavHostController,
    historyRepository: HistoryRepository,
    onManualSync: () -> Unit
) {
    val insightsEngine = remember { BehaviorInsightsEngine() }
    val snapshot = insightsEngine.build(summaries)
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Extract last 7 days of sedentary minutes for the chart
    val trendData = remember(summaries) {
        summaries.takeLast(7).map { it.sedentaryMinutes }
    }

    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader { Text("Smartr", style = MaterialTheme.typography.titleMedium) }
            }

            item {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        PassiveRuntimeStore.reset()
                        scope.launch {
                            historyRepository.recordReminderAcknowledged(LocalDate.now())
                            ComplicationUpdater.updateAll(context)
                            Toast.makeText(context, "Break recorded!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("✅ Mark as Done")
                }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onManualSync()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now")
                    }
                }
            }

            item {
                val scoreColor = when {
                    snapshot.wellnessScore > 80 -> colorResource(R.color.wellness_high)
                    snapshot.wellnessScore > 50 -> colorResource(R.color.wellness_mid)
                    else -> colorResource(R.color.wellness_low)
                }

                TitleCard(
                    onClick = { },
                    title = { Text("Wellness Score") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleColor = scoreColor
                    ),
                    subtitle = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { snapshot.wellnessScore / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    colors = ProgressIndicatorDefaults.colors(
                                        indicatorColor = scoreColor,
                                        trackColor = scoreColor.copy(alpha = 0.2f)
                                    ),
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "${snapshot.wellnessScore}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "🔥 ${snapshot.currentStreak} day streak",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                ) {
                    Text("You're doing great! Keep moving.", style = MaterialTheme.typography.labelSmall)
                }
            }

            item {
                val scoreColor = when {
                    snapshot.wellnessScore > 80 -> colorResource(R.color.wellness_high)
                    snapshot.wellnessScore > 50 -> colorResource(R.color.wellness_mid)
                    else -> colorResource(R.color.wellness_low)
                }

                AppCard(
                    onClick = { navController.navigate(Screen.History.route) },
                    appName = { Text("Activity Insights") },
                    appImage = { 
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = scoreColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    title = { Text("7-Day Trend") },
                    time = { Text("Last 7d") },
                ) {
                    Column {
                        Text(
                            "Avg Sedentary: ${snapshot.averageSedentaryMinutes}m",
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
                                "Collect more data for chart",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { navController.navigate(Screen.History.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("View History")
                }
            }

            item {
                Button(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(summaries: List<DailySummary>) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text("History", style = MaterialTheme.typography.titleMedium) } }
            
            if (summaries.isEmpty()) {
                item { Text("No history yet", style = MaterialTheme.typography.bodyMedium) }
            }

            items(summaries.take(10)) { summary ->
                TitleCard(
                    onClick = { },
                    title = { Text(summary.dateIso) },
                    subtitle = { Text("${summary.sedentaryMinutes}m sitting") }
                ) {
                    Text("${summary.remindersSent} reminders", style = MaterialTheme.typography.labelSmall)
                }
            }

            item {
                Text(
                    "Full history available on your phone.",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository
) {
    var activeEditor by remember { mutableStateOf(SettingType.NONE) }
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    if (activeEditor != SettingType.NONE) {
        BackHandler { activeEditor = SettingType.NONE }
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeEditor) {
                SettingType.SIT_LIMIT -> {
                    val unit = settings.sitThresholdUnit
                    val range = when (unit) {
                        TimeIntervalUnit.SECONDS -> 30..3600 step 30
                        TimeIntervalUnit.MINUTES -> 1..240 step 1
                        TimeIntervalUnit.HOURS -> 1..12 step 1
                    }
                    Stepper(
                        value = settings.sitThresholdValue.coerceIn(range.first, range.last),
                        onValueChange = { scope.launch { repository.updateSitThresholdValue(it) } },
                        valueProgression = range,
                        increaseIcon = { Icon(Icons.Default.Add, "Increase") },
                        decreaseIcon = { Icon(Icons.Default.Remove, "Decrease") }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sit Limit", style = MaterialTheme.typography.labelMedium)
                            Text("${settings.sitThresholdValue}", style = MaterialTheme.typography.displayMedium)
                            FilledTonalButton(
                                onClick = {
                                    val nextUnit = TimeIntervalUnit.entries[(unit.ordinal + 1) % TimeIntervalUnit.entries.size]
                                    scope.launch { repository.updateSitThresholdUnit(nextUnit) }
                                },
                                modifier = Modifier.size(width = 80.dp, height = 32.dp)
                            ) {
                                Text(unit.name.lowercase(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                SettingType.REMINDER_REPEAT -> {
                    val unit = settings.reminderRepeatUnit
                    val range = when (unit) {
                        TimeIntervalUnit.SECONDS -> 15..600 step 15
                        TimeIntervalUnit.MINUTES -> 1..120 step 1
                        TimeIntervalUnit.HOURS -> 1..4 step 1
                    }
                    Stepper(
                        value = settings.reminderRepeatValue.coerceIn(range.first, range.last),
                        onValueChange = { scope.launch { repository.updateReminderRepeatValue(it) } },
                        valueProgression = range,
                        increaseIcon = { Icon(Icons.Default.Add, "Increase") },
                        decreaseIcon = { Icon(Icons.Default.Remove, "Decrease") }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Reminder", style = MaterialTheme.typography.labelMedium)
                            Text("${settings.reminderRepeatValue}", style = MaterialTheme.typography.displayMedium)
                            FilledTonalButton(
                                onClick = {
                                    val nextUnit = TimeIntervalUnit.entries[(unit.ordinal + 1) % TimeIntervalUnit.entries.size]
                                    scope.launch { repository.updateReminderRepeatUnit(nextUnit) }
                                },
                                modifier = Modifier.size(width = 80.dp, height = 32.dp)
                            ) {
                                Text(unit.name.lowercase(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                SettingType.QUIET_START -> {
                    TimePicker(
                        initialTime = LocalTime.of(settings.quietStartHour, 0),
                        onTimePicked = {
                            scope.launch {
                                repository.updateQuietStartHour(it.hour)
                                activeEditor = SettingType.NONE
                            }
                        }
                    )
                }
                SettingType.QUIET_END -> {
                    TimePicker(
                        initialTime = LocalTime.of(settings.quietEndHour, 0),
                        onTimePicked = {
                            scope.launch {
                                repository.updateQuietEndHour(it.hour)
                                activeEditor = SettingType.NONE
                            }
                        }
                    )
                }
                SettingType.THEME -> {
                    ScalingLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item { ListHeader { Text("Theme") } }
                        ThemePreference.entries.forEach { theme ->
                            item {
                                TitleCard(
                                    onClick = { 
                                        scope.launch { 
                                            repository.updateTheme(theme)
                                            activeEditor = SettingType.NONE
                                        } 
                                    },
                                    title = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                ) {
                                    RadioButton(
                                        selected = settings.theme == theme,
                                        onSelect = {
                                            scope.launch {
                                                repository.updateTheme(theme)
                                                activeEditor = SettingType.NONE
                                            }
                                        },
                                        label = { Text("Select") }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    } else {
        ScreenScaffold(scrollState = listState, timeText = { TimeText() }) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { ListHeader { Text("Settings") } }

                item {
                    TitleCard(
                        onClick = { activeEditor = SettingType.SIT_LIMIT },
                        title = { Text("Sit Limit") },
                        subtitle = { 
                            val unitStr = settings.sitThresholdUnit.name.lowercase().removeSuffix("s")
                            Text("${settings.sitThresholdValue} $unitStr${if (settings.sitThresholdValue > 1) "s" else ""}") 
                        }
                    )
                }

                item {
                    TitleCard(
                        onClick = { activeEditor = SettingType.REMINDER_REPEAT },
                        title = { Text("Reminder Every") },
                        subtitle = { 
                            val unitStr = settings.reminderRepeatUnit.name.lowercase().removeSuffix("s")
                            Text("${settings.reminderRepeatValue} $unitStr${if (settings.reminderRepeatValue > 1) "s" else ""}")
                        }
                    )
                }

                item {
                    TitleCard(
                        onClick = { },
                        title = { Text("Quiet Hours") },
                        subtitle = { Text("${settings.quietStartHour}:00 - ${settings.quietEndHour}:00") }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { activeEditor = SettingType.QUIET_START },
                                modifier = Modifier.weight(1f)
                            ) { Text("Start") }
                            FilledTonalButton(
                                onClick = { activeEditor = SettingType.QUIET_END },
                                modifier = Modifier.weight(1f)
                            ) { Text("End") }
                        }
                    }
                }

                item {
                    TitleCard(
                        onClick = { activeEditor = SettingType.THEME },
                        title = { Text("Theme") },
                        subtitle = { Text(settings.theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }
    }
}
