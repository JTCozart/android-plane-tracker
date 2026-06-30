package com.jtcozart.planetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.data.SettingsRepository
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.data.TrackerStateHolder
import com.jtcozart.planetracker.notify.Notifier
import com.jtcozart.planetracker.service.TrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val notifier = Notifier(app)

    val state: StateFlow<TrackerState> = TrackerStateHolder.state

    val settings: StateFlow<Settings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings(),
    )

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    fun startTracking() = TrackingService.start(getApplication())
    fun stopTracking() = TrackingService.stop(getApplication())

    fun toggleLocationLock() = TrackerStateHolder.update { it.copy(locationLocked = !it.locationLocked) }

    fun sendTestNotification() = notifier.sendTest()
}
