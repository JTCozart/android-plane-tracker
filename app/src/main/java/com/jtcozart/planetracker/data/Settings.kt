package com.jtcozart.planetracker.data

import com.jtcozart.planetracker.model.AircraftClass

/**
 * User-tunable settings. Replaces the firmware Config struct / NVS, minus the pieces
 * that don't apply on a phone (WiFi creds, web password, OTA, ntfy token/topic).
 */
data class Settings(
    val radiusNm: Float = DEFAULT_RADIUS_NM,
    val pollIntervalSec: Int = DEFAULT_POLL_INTERVAL_SEC,

    // POI display filter (only show these ICAO type codes)
    val poiTypes: String = "",
    val poiEnabled: Boolean = false,

    // Notification categories (mirror the firmware's ntfy categories)
    val notificationsEnabled: Boolean = true,
    val notifyMilitary: Boolean = true,
    val notifyMedevac: Boolean = true,
    val notifyCommercial: Boolean = true,
    val notifyPrivate: Boolean = true,
    val notifyPoi: Boolean = false,             // overrides class filter when POI display is active
    val notifyEmergencySquawk: Boolean = true,
) {
    val poiDisplayActive: Boolean get() = poiEnabled && poiTypes.isNotBlank()
    val poiNotifyActive: Boolean get() = notifyPoi && poiTypes.isNotBlank()

    /** Whether a detection of this class should fire a notification. */
    fun notifiesClass(cls: AircraftClass): Boolean {
        if (!notificationsEnabled) return false
        if (poiNotifyActive) return true // POI bypasses the class filter
        return when (cls) {
            AircraftClass.MILITARY -> notifyMilitary
            AircraftClass.MEDEVAC -> notifyMedevac
            AircraftClass.COMMERCIAL -> notifyCommercial
            AircraftClass.PRIVATE -> notifyPrivate
        }
    }

    companion object {
        const val DEFAULT_RADIUS_NM = 5.0f
        const val DEFAULT_POLL_INTERVAL_SEC = 30
        const val MIN_POLL_INTERVAL_SEC = 10
    }
}
