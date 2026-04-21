package com.smartr.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.smartr.wear.data.SettingsRepository
import com.smartr.wear.data.history.HistoryRepository
import com.smartr.wear.logic.BehaviorInsightsEngine
import com.smartr.wear.logic.PassiveRuntimeStore
import com.smartr.wear.worker.PassiveRegistrationWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

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
            val insightsEngine = remember { BehaviorInsightsEngine() }
            val snapshot = insightsEngine.build(summaries)
            val scope = rememberCoroutineScope()
            val nowTick by produceState(initialValue = Instant.now()) {
                while (true) {
                    value = Instant.now()
                    delay(30_000)
                }
            }
            val lastUpdateText = PassiveRuntimeStore.lastPassiveCallbackAt?.let {
                val minutes = Duration.between(it, nowTick).toMinutes().coerceAtLeast(0)
                "Last update: ${minutes}m ago"
            } ?: "Waiting for data..."

            MaterialTheme {
                val listState = rememberScalingLazyListState()
                Scaffold(
                    timeText = { TimeText() }
                ) {
                    ScalingLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            top = 32.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 32.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            ListHeader {
                                Text("Smartr", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        // Insights Card
                        item {
                            AppCard(
                                onClick = { },
                                appName = { Text("Daily Insight") },
                                time = { },
                                title = { Text("Activity Summary") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("Avg sit: ${snapshot.averageSedentaryMinutes}m", style = MaterialTheme.typography.bodySmall)
                                    Text("Response: ${snapshot.reminderResponseRate}%", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        item {
                            ListHeader {
                                Text("Settings", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Sit Limit Setting
                        item {
                            TitleCard(
                                onClick = { },
                                title = { Text("Sit Limit") },
                                subtitle = { Text("${settings.sitThresholdMinutes} minutes") }
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { scope.launch { settingsRepository.updateSitThreshold(settings.sitThresholdMinutes - 5) } },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("-5") }
                                    FilledTonalButton(
                                        onClick = { scope.launch { settingsRepository.updateSitThreshold(settings.sitThresholdMinutes + 5) } },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("+5") }
                                }
                            }
                        }

                        // Repeat Setting
                        item {
                            TitleCard(
                                onClick = { },
                                title = { Text("Reminder Every") },
                                subtitle = { Text("${settings.reminderRepeatMinutes} minutes") }
                            ) {
                                FilledTonalButton(
                                    onClick = { scope.launch { settingsRepository.updateReminderRepeat(settings.reminderRepeatMinutes + 5) } },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("+5 Minutes") }
                            }
                        }

                        // Quiet Hours
                        item {
                            TitleCard(
                                onClick = { },
                                title = { Text("Quiet Hours") },
                                subtitle = { Text("${settings.quietStartHour}:00 - ${settings.quietEndHour}:00") }
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { scope.launch { settingsRepository.updateQuietStartHour((settings.quietStartHour + 1) % 24) } },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Start") }
                                    FilledTonalButton(
                                        onClick = { scope.launch { settingsRepository.updateQuietEndHour((settings.quietEndHour + 1) % 24) } },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("End") }
                                }
                            }
                        }

                        item {
                            Text(
                                text = lastUpdateText,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
