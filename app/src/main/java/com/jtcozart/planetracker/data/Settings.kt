package com.jtcozart.planetracker.data

import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.model.AircraftClass

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * User-tunable settings. Replaces the firmware Config struct / NVS, minus the pieces
 * that don't apply on a phone (WiFi creds, web password, OTA, ntfy token/topic).
 */
data class Settings(
    val radiusNm: Float = DEFAULT_RADIUS_NM,
    val pollIntervalSec: Int = DEFAULT_POLL_INTERVAL_SEC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    // POI alerting (page me for these ICAO type codes regardless of class filters)
    val poiTypes: String = "",

    // Notification categories (mirror the firmware's ntfy categories)
    val notificationsEnabled: Boolean = true,
    val notifyMilitary: Boolean = true,
    val notifyMedevac: Boolean = true,
    val notifyCommercial: Boolean = true,
    val notifyPrivate: Boolean = true,
    val notifyPoi: Boolean = false,             // overrides class filter when POI alerting is active
    val notifyEmergencySquawk: Boolean = true,
    val notifyStreakReminder: Boolean = true,
) {
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

    /** Whether this aircraft matches the active notification filters (used by the "notify matches only" display filter). */
    fun notifiesAircraft(aircraft: Aircraft): Boolean =
        aircraft.isEmergencySquawk || notifiesClass(aircraft.classification)

    companion object {
        const val DEFAULT_RADIUS_NM = 5.0f
        const val DEFAULT_POLL_INTERVAL_SEC = 30
        const val MIN_POLL_INTERVAL_SEC = 10
    }
}
