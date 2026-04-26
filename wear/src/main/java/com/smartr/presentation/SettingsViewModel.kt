package com.smartr.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartr.data.AppSettings
import com.smartr.data.SettingsRepository
import com.smartr.data.ThemePreference
import com.smartr.data.TimeIntervalUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

import com.smartr.data.history.HistoryRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val historyRepository = HistoryRepository(application)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULTS)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun updateSitThreshold(value: Int, unit: TimeIntervalUnit) {
        viewModelScope.launch { repository.updateSitThreshold(value, unit) }
    }

    fun updateReminderRepeat(value: Int, unit: TimeIntervalUnit) {
        viewModelScope.launch { repository.updateReminderRepeat(value, unit) }
    }

    fun updateMovementBuffer(value: Int, unit: TimeIntervalUnit) {
        viewModelScope.launch { repository.updateMovementBuffer(value, unit) }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch { repository.updateTheme(theme) }
    }

    fun updateQuietStartHour(hour: Int) {
        viewModelScope.launch { repository.updateQuietStartHour(hour) }
    }

    fun updateQuietEndHour(hour: Int) {
        viewModelScope.launch { repository.updateQuietEndHour(hour) }
    }

    fun injectMockData(scenario: String) {
        viewModelScope.launch {
            historyRepository.injectMockScenario(scenario)
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                val nodes = com.google.android.gms.wearable.Wearable.getNodeClient(getApplication()).connectedNodes.await()
                for (node in nodes) {
                    com.google.android.gms.wearable.Wearable.getMessageClient(getApplication())
                        .sendMessage(node.id, "/ping", "ping_from_watch".toByteArray())
                        .await()
                }
                // Simulate some work for better UX/feedback if sync is too fast
                delay(2000)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to trigger sync", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
