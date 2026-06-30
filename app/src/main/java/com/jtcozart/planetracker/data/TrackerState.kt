package com.jtcozart.planetracker.data

import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.model.AircraftClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Immutable snapshot of everything the UI renders. Produced by the service. */
data class TrackerState(
    val running: Boolean = false,
    val active: List<Aircraft> = emptyList(),
    val history: List<Aircraft> = emptyList(),
    val counts: Map<AircraftClass, Int> = AircraftClass.entries.associateWith { 0 },
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val radiusNm: Float = Settings.DEFAULT_RADIUS_NM,
    val lastHttpCode: Int = 0,
    val lastAircraftCount: Int = 0,
    val consecutiveFailures: Int = 0,
    val lastUpdateMillis: Long = 0,
    val locationLocked: Boolean = false,
) {
    val hasActiveAircraft: Boolean get() = active.isNotEmpty()
    val hasLocation: Boolean get() = centerLat != null && centerLon != null
}

/**
 * Single source of truth shared between the [com.jtcozart.planetracker.service.TrackingService]
 * (writer) and the UI (reader). A process-level singleton avoids binding plumbing.
 */
object TrackerStateHolder {
    private val _state = MutableStateFlow(TrackerState())
    val state: StateFlow<TrackerState> = _state.asStateFlow()

    fun update(transform: (TrackerState) -> TrackerState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = TrackerState()
    }
}
