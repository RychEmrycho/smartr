package com.smartr.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.smartr.wear.data.SettingsRepository
import com.smartr.wear.worker.PassiveRegistrationWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = SettingsRepository(applicationContext)

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
            val scope = rememberCoroutineScope()
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Smartr")
                    Text("Sit limit: ${settings.sitThresholdMinutes} min")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            modifier = Modifier.width(120.dp),
                            onClick = {
                                scope.launch {
                                    settingsRepository.updateSitThreshold(settings.sitThresholdMinutes + 5)
                                }
                            }
                        ) { Text("Sit +5") }
                        Button(
                            modifier = Modifier.width(120.dp),
                            onClick = {
                                scope.launch {
                                    settingsRepository.updateSitThreshold(settings.sitThresholdMinutes - 5)
                                }
                            }
                        ) { Text("Sit -5") }
                    }
                    Text("Repeat: ${settings.reminderRepeatMinutes} min")
                    Button(
                        modifier = Modifier.width(120.dp),
                        onClick = {
                            scope.launch {
                                settingsRepository.updateReminderRepeat(settings.reminderRepeatMinutes + 5)
                            }
                        }
                    ) { Text("Repeat +5") }
                    Text("Quiet: ${settings.quietStartHour}:00-${settings.quietEndHour}:00")
                    Button(
                        modifier = Modifier.width(120.dp),
                        onClick = {
                            scope.launch {
                                settingsRepository.updateQuietStartHour((settings.quietStartHour + 1) % 24)
                            }
                        }
                    ) { Text("Start +1h") }
                    Button(
                        modifier = Modifier.width(120.dp),
                        onClick = {
                            scope.launch {
                                settingsRepository.updateQuietEndHour((settings.quietEndHour + 1) % 24)
                            }
                        }
                    ) { Text("End +1h") }
                }
            }
        }
    }
}
