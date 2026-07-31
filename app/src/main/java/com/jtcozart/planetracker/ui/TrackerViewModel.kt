package com.jtcozart.planetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jtcozart.planetracker.data.HistoryRepository
import com.jtcozart.planetracker.data.OnboardingRepository
import com.jtcozart.planetracker.data.ReviewPromptRepository
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.data.SettingsRepository
import com.jtcozart.planetracker.data.SpottedAircraft
import com.jtcozart.planetracker.data.SpottedRepository
import com.jtcozart.planetracker.data.StreakRepository
import com.jtcozart.planetracker.data.StreakState
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.data.TrackerStateHolder
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.notify.Notifier
import com.jtcozart.planetracker.service.TrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val historyRepo = HistoryRepository(app)
    private val onboardingRepo = OnboardingRepository(app)
    private val reviewPromptRepo = ReviewPromptRepository(app)
    private val streakRepo = StreakRepository(app)
    private val spottedRepo = SpottedRepository(app)
    private val notifier = Notifier(app)

    init {
        viewModelScope.launch { reviewPromptRepo.recordFirstLaunchIfNeeded() }
    }

    val streak: StateFlow<StreakState> = streakRepo.streak.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakState(),
    )

    val spotted: StateFlow<List<SpottedAircraft>> = spottedRepo.spotted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun markSpotted(aircraft: Aircraft) {
        viewModelScope.launch {
            spottedRepo.add(aircraft)
            streakRepo.recordSpotToday()
        }
    }

    val state: StateFlow<TrackerState> = TrackerStateHolder.state

    val settings: StateFlow<Settings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings(),
    )

    val tutorialCompleted: StateFlow<Boolean> = onboardingRepo.tutorialCompleted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true,
    )

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    fun completeTutorial() {
        viewModelScope.launch { onboardingRepo.setTutorialCompleted() }
    }

    val shouldShowReviewPrompt: StateFlow<Boolean> = reviewPromptRepo.shouldShowPrompt.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    fun dismissReviewPrompt() {
        viewModelScope.launch { reviewPromptRepo.setPromptHandled() }
    }

    fun startTracking() = TrackingService.start(getApplication())
    fun stopTracking() = TrackingService.stop(getApplication())

    fun toggleLocationLock() = TrackerStateHolder.update { it.copy(locationLocked = !it.locationLocked) }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clear() }
        TrackerStateHolder.update { it.copy(history = emptyList(), counts = com.jtcozart.planetracker.model.AircraftClass.entries.associateWith { 0 }) }
        TrackerStateHolder.signalClearHistory()
    }

    fun sendTestNotification() = notifier.sendTest()
}
