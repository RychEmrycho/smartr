package com.smartr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartr.data.MobileSettingsRepository
import com.smartr.data.WatchSettings
import com.smartr.data.history.MobileHistoryRepository
import com.smartr.logic.BehaviorInsightsEngine
import com.smartr.logic.InsightSnapshot
import com.smartr.ui.components.TrendChart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import com.smartr.data.health.HealthConnectManager
import androidx.work.*
import com.smartr.service.SleepDetectionWorker
import com.smartr.service.SyncPingWorker
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = MobileSettingsRepository(applicationContext)
        val historyRepository = MobileHistoryRepository(applicationContext)
        val healthManager = HealthConnectManager(applicationContext)
        val engine = BehaviorInsightsEngine()

        setContent {
            val settings by settingsRepository.watchSettings.collectAsState(initial = MobileSettingsRepository.DEFAULTS)
            val history by historyRepository.summaries(30).collectAsState(initial = emptyList())
            val insight = remember(history) { engine.build(history) }
            
            var hasHealthPermissions by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                scheduleSyncPing()
                hasHealthPermissions = healthManager.hasAllPermissions()
                if (hasHealthPermissions) {
                    scheduleSleepWorker()
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                PermissionController.createRequestPermissionResultContract()
            ) { granted: Set<String> ->
                hasHealthPermissions = granted.containsAll(healthManager.permissions)
                if (hasHealthPermissions) {
                    scheduleSleepWorker()
                }
            }

            val scope = rememberCoroutineScope()

            SmartrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        insight = insight,
                        trend = history.map { it.sedentaryMinutes }.reversed(),
                        settings = settings,
                        hasHealthPermissions = hasHealthPermissions,
                        onConnectHealth = {
                            permissionLauncher.launch(healthManager.permissions)
                        },
                        onTestConnectivity = {
                            sendPing()
                        },
                        onUpdatePing = { v, unit ->
                            scope.launch {
                                settingsRepository.updatePingFrequency(v, unit)
                                scheduleSyncPing() // Restart with new frequency
                            }
                        }
                    )
                }
            }
        }
    }

    private fun scheduleSleepWorker() {
        val request = PeriodicWorkRequestBuilder<SleepDetectionWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sleep_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleSyncPing() {
        val request = OneTimeWorkRequestBuilder<SyncPingWorker>()
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "sync_ping",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun sendPing() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val nodes = com.google.android.gms.tasks.Tasks.await(nodeClient.connectedNodes)
                for (node in nodes) {
                    val messageClient = Wearable.getMessageClient(this@MainActivity)
                    com.google.android.gms.tasks.Tasks.await(messageClient.sendMessage(node.id, "/ping", "ping_manual".toByteArray()))
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Manual ping failed", e)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    insight: InsightSnapshot,
    trend: List<Int>,
    settings: WatchSettings,
    hasHealthPermissions: Boolean,
    onConnectHealth: () -> Unit,
    onTestConnectivity: () -> Unit,
    onUpdatePing: (Int, com.smartr.data.TimeIntervalUnit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HeaderSection(onTestConnectivity)
        }

        if (BuildConfig.DEBUG) {
            item {
                ConnectivityDebugCard(settings, onUpdatePing)
            }
        }

        if (!hasHealthPermissions) {
            item {
                HealthConnectPrompt(onConnectHealth)
            }
        }

        item {
            WellnessScoreCard(insight.wellnessScore)
        }

        item {
            StatsGrid(insight)
        }

        item {
            TrendSection(trend)
        }

        item {
            WatchSettingsCard(settings)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Last synced: Just now",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HeaderSection(onTestConnectivity: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Smartr",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Health Hub",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTestConnectivity) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = "Test Connectivity",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Watch, contentDescription = null, tint = Color(0xFF81C784))
            }
        }
    }
}

@Composable
fun WellnessScoreCard(score: Int) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF81C784)
        score >= 50 -> Color(0xFFFFD54F)
        else -> Color(0xFFE57373)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(200.dp),
                    color = scoreColor,
                    strokeWidth = 16.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wellness Score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatsGrid(insight: InsightSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            label = "Current Streak",
            value = "${insight.currentStreak} Days",
            icon = Icons.Default.Whatshot,
            color = Color(0xFFFF8A65),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Response Rate",
            value = "${insight.reminderResponseRate}%",
            icon = Icons.Default.Bolt,
            color = Color(0xFF64B5F6),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrendSection(trend: List<Int>) {
    Column {
        Text(
            text = "Activity Trend (30 Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))
        ) {
            TrendChart(
                data = if (trend.isEmpty()) listOf(0, 0) else trend,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun WatchSettingsCard(settings: WatchSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Settings", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val unitLabel = { value: Int, unit: com.smartr.data.TimeIntervalUnit -> "$value ${unit.name.lowercase()}" }
            
            SettingRowLite("Sit Limit", unitLabel(settings.thresholdValue, settings.thresholdUnit))
            SettingRowLite("Reminder", unitLabel(settings.repeatValue, settings.repeatUnit))
            SettingRowLite("Quiet Hours", "${settings.quietStartHour}:00 - ${settings.quietEndHour}:00")
        }
    }
}

@Composable
fun SettingRowLite(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HealthConnectPrompt(onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
        onClick = onConnect
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.HealthAndSafety,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Connect Health Intelligence",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Enable sleep awareness and data syncing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ConnectivityDebugCard(
    settings: WatchSettings,
    onUpdatePing: (Int, com.smartr.data.TimeIntervalUnit) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Connectivity Debug",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Ping Frequency",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val values = listOf(10, 30, 60, 15, 1)
            val units = listOf(
                com.smartr.data.TimeIntervalUnit.SECONDS,
                com.smartr.data.TimeIntervalUnit.SECONDS,
                com.smartr.data.TimeIntervalUnit.SECONDS,
                com.smartr.data.TimeIntervalUnit.MINUTES,
                com.smartr.data.TimeIntervalUnit.HOURS
            )
            
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(values.size) { index ->
                    val v = values[index]
                    val u = units[index]
                    val isSelected = settings.pingValue == v && settings.pingUnit == u
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdatePing(v, u) },
                        label = { 
                            Text(
                                text = "$v ${u.name.lowercase().take(1)}",
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Frequencies < 15m use recursive workers.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SmartrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFFCCC2DC),
            tertiary = Color(0xFFEFB8C8),
            background = Color(0xFF0F0F0F),
            surface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFF2C2B2F)
        ),
        typography = Typography(),
        content = content
    )
}
