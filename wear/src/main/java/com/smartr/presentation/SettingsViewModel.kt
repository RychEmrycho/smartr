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

    fun updateSitThresholdValue(value: Int) {
        viewModelScope.launch { repository.updateSitThresholdValue(value) }
    }

    fun updateSitThresholdUnit(unit: TimeIntervalUnit) {
        viewModelScope.launch { repository.updateSitThresholdUnit(unit) }
    }

    fun updateReminderRepeatValue(value: Int) {
        viewModelScope.launch { repository.updateReminderRepeatValue(value) }
    }

    fun updateReminderRepeatUnit(unit: TimeIntervalUnit) {
        viewModelScope.launch { repository.updateReminderRepeatUnit(unit) }
    }

    fun updateQuietStartHour(hour: Int) {
        viewModelScope.launch { repository.updateQuietStartHour(hour) }
    }

    fun updateQuietEndHour(hour: Int) {
        viewModelScope.launch { repository.updateQuietEndHour(hour) }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch { repository.updateTheme(theme) }
    }
}
