package com.smartr.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartr.complication.ComplicationUpdater
import com.smartr.data.AppSettings
import com.smartr.data.SettingsRepository
import com.smartr.data.TrackingStateRepository
import com.smartr.data.history.PersonalBest
import com.smartr.data.history.DailySummary
import com.smartr.data.history.HistoryRepository
import com.smartr.logic.BehaviorInsightsEngine
import com.smartr.logic.InsightSnapshot
import com.smartr.logic.PassiveRuntimeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val historyRepository = HistoryRepository(application)
    private val trackingRepository = TrackingStateRepository(application)
    private val insightsEngine = BehaviorInsightsEngine()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULTS)

    val summaries: StateFlow<List<DailySummary>> = historyRepository.summaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insights: StateFlow<InsightSnapshot> = summaries
        .map { insightsEngine.build(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightSnapshot(0, 0, 100, 100, 0, 1, 0f, "Novice", 0, null, null, "B"))

    val personalBests: StateFlow<List<PersonalBest>> = historyRepository.personalBests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendData: StateFlow<List<Int>> = summaries
        .map { list -> list.takeLast(7).map { it.sedentaryMinutes } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsDone() {
        viewModelScope.launch {
            PassiveRuntimeStore.reset()
            trackingRepository.reset()
            historyRepository.recordReminderAcknowledged(LocalDate.now())
            ComplicationUpdater.updateAll(getApplication())
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nodes = com.google.android.gms.wearable.Wearable.getNodeClient(getApplication()).connectedNodes.await()
                for (node in nodes) {
                    com.google.android.gms.wearable.Wearable.getMessageClient(getApplication())
                        .sendMessage(node.id, "/ping", "ping_from_watch".toByteArray())
                        .await()
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Failed to trigger sync", e)
            }
        }
    }
}
