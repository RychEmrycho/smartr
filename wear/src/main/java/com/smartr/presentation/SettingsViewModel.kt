package com.smartr.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartr.data.AppSettings
import com.smartr.data.SettingsRepository
import com.smartr.data.ThemePreference
import com.smartr.data.TimeIntervalUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULTS)

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
}
