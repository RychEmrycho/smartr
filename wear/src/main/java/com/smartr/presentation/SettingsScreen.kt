package com.smartr.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.smartr.data.ThemePreference
import com.smartr.data.TimeIntervalUnit
import java.time.LocalTime
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.smartr.R

enum class SettingType {
    NONE, SIT_LIMIT, REMINDER_REPEAT, QUIET_START, QUIET_END, MOVEMENT_BUFFER, THEME
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
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
                        onValueChange = { viewModel.updateSitThreshold(it, unit) },
                        valueProgression = range,
                        increaseIcon = { Icon(Icons.Default.Add, stringResource(R.string.settings_increase)) },
                        decreaseIcon = { Icon(Icons.Default.Remove, stringResource(R.string.settings_decrease)) }
                    ) {
                        val unitLabel = unit.name.lowercase().take(3)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_sit_limit),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            TextButton(
                                onClick = {
                                    val nextUnit = TimeIntervalUnit.entries[(unit.ordinal + 1) % TimeIntervalUnit.entries.size]
                                    viewModel.updateSitThreshold(settings.sitThresholdValue, nextUnit)
                                }
                            ) {
                                Text(
                                    text = "${settings.sitThresholdValue} $unitLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
                        onValueChange = { viewModel.updateReminderRepeat(it, unit) },
                        valueProgression = range,
                        increaseIcon = { Icon(Icons.Default.Add, stringResource(R.string.settings_increase)) },
                        decreaseIcon = { Icon(Icons.Default.Remove, stringResource(R.string.settings_decrease)) }
                    ) {
                        val unitLabel = unit.name.lowercase().take(3)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_reminder_every),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            TextButton(
                                onClick = {
                                    val nextUnit = TimeIntervalUnit.entries[(unit.ordinal + 1) % TimeIntervalUnit.entries.size]
                                    viewModel.updateReminderRepeat(settings.reminderRepeatValue, nextUnit)
                                }
                            ) {
                                Text(
                                    text = "${settings.reminderRepeatValue} $unitLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                SettingType.QUIET_START -> {
                    TimePicker(
                        initialTime = LocalTime.of(settings.quietStartHour, 0),
                        onTimePicked = {
                            scope.launch {
                                viewModel.updateQuietStartHour(it.hour)
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
                                viewModel.updateQuietEndHour(it.hour)
                                activeEditor = SettingType.NONE
                            }
                        }
                    )
                }
                SettingType.MOVEMENT_BUFFER -> {
                    val unit = settings.movementBufferUnit
                    val range = when (unit) {
                        TimeIntervalUnit.SECONDS -> 5..300 step 5
                        else -> 1..10 step 1 // Only Seconds and Minutes make sense for a buffer
                    }
                    Stepper(
                        value = settings.movementBufferValue.coerceIn(range.first, range.last),
                        onValueChange = { viewModel.updateMovementBuffer(it, unit) },
                        valueProgression = range,
                        increaseIcon = { Icon(Icons.Default.Add, stringResource(R.string.settings_increase)) },
                        decreaseIcon = { Icon(Icons.Default.Remove, stringResource(R.string.settings_decrease)) }
                    ) {
                        val unitLabel = unit.name.lowercase().take(3)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_movement_buffer),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            TextButton(
                                onClick = {
                                    val nextUnit = if (unit == TimeIntervalUnit.SECONDS) TimeIntervalUnit.MINUTES else TimeIntervalUnit.SECONDS
                                    viewModel.updateMovementBuffer(settings.movementBufferValue, nextUnit)
                                }
                            ) {
                                Text(
                                    text = "${settings.movementBufferValue} $unitLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                SettingType.THEME -> {
                    ScalingLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item { ListHeader { Text(stringResource(R.string.settings_theme)) } }
                        ThemePreference.entries.forEach { theme ->
                            item(key = theme.name) {
                                TitleCard(
                                    onClick = { 
                                        scope.launch { 
                                            viewModel.updateTheme(theme)
                                            activeEditor = SettingType.NONE
                                        } 
                                    },
                                    title = { 
                                        val displayName = when (theme) {
                                            ThemePreference.AUTO -> stringResource(R.string.theme_auto)
                                            ThemePreference.LIGHT -> stringResource(R.string.theme_light)
                                            ThemePreference.DARK -> stringResource(R.string.theme_dark)
                                        }
                                        Text(displayName)
                                    }
                                ) {
                                    RadioButton(
                                        selected = settings.theme == theme,
                                        onSelect = {
                                            scope.launch {
                                                viewModel.updateTheme(theme)
                                                activeEditor = SettingType.NONE
                                            }
                                        },
                                        label = { Text(stringResource(R.string.settings_select)) }
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
                item { ListHeader { Text(stringResource(R.string.settings_title)) } }

                item {
                    val unitStr = settings.sitThresholdUnit.name.lowercase().removeSuffix("s")
                    val subtitle = if (settings.sitThresholdValue > 1) {
                        stringResource(R.string.settings_format_plural, settings.sitThresholdValue, unitStr)
                    } else {
                        stringResource(R.string.settings_format_singular, settings.sitThresholdValue, unitStr)
                    }
                    TitleCard(
                        onClick = { activeEditor = SettingType.SIT_LIMIT },
                        title = { Text(stringResource(R.string.settings_sit_limit)) },
                        subtitle = { Text(subtitle) }
                    )
                }

                item {
                    val unitStr = settings.reminderRepeatUnit.name.lowercase().removeSuffix("s")
                    val subtitle = if (settings.reminderRepeatValue > 1) {
                        stringResource(R.string.settings_format_plural, settings.reminderRepeatValue, unitStr)
                    } else {
                        stringResource(R.string.settings_format_singular, settings.reminderRepeatValue, unitStr)
                    }
                    TitleCard(
                        onClick = { activeEditor = SettingType.REMINDER_REPEAT },
                        title = { Text(stringResource(R.string.settings_reminder_every)) },
                        subtitle = { Text(subtitle) }
                    )
                }

                item {
                    TitleCard(
                        onClick = { },
                        title = { Text(stringResource(R.string.settings_quiet_hours)) },
                        subtitle = { Text("${settings.quietStartHour}:00 - ${settings.quietEndHour}:00") }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { activeEditor = SettingType.QUIET_START },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.settings_start)) }
                            FilledTonalButton(
                                onClick = { activeEditor = SettingType.QUIET_END },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.settings_end)) }
                        }
                    }
                }

                item {
                    TitleCard(
                        onClick = { activeEditor = SettingType.THEME },
                        title = { Text(stringResource(R.string.settings_theme)) },
                        subtitle = { 
                            val displayName = when (settings.theme) {
                                ThemePreference.AUTO -> stringResource(R.string.theme_auto)
                                ThemePreference.LIGHT -> stringResource(R.string.theme_light)
                                ThemePreference.DARK -> stringResource(R.string.theme_dark)
                            }
                            Text(displayName)
                        }
                    )
                }

                item {
                    val unitStr = settings.movementBufferUnit.name.lowercase().removeSuffix("s")
                    val subtitle = if (settings.movementBufferValue > 1) {
                        stringResource(R.string.settings_format_plural, settings.movementBufferValue, unitStr)
                    } else {
                        stringResource(R.string.settings_format_singular, settings.movementBufferValue, unitStr)
                    }
                    TitleCard(
                        onClick = { activeEditor = SettingType.MOVEMENT_BUFFER },
                        title = { Text(stringResource(R.string.settings_movement_buffer)) },
                        subtitle = { Text(subtitle) }
                    )
                }
            }
        }
    }
}
