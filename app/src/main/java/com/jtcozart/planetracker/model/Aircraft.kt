package com.jtcozart.planetracker.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A single tracked aircraft. Geometry helpers (distance / bearing / ETA / approaching)
 * are direct ports of the firmware's Aircraft.cpp.
 */
data class Aircraft(
    val icao: String,
    val callsign: String,
    val registration: String,   // tail number
    val type: String,           // ICAO type code
    val owner: String,
    val squawk: String,
    val altitude: Float,        // barometric feet
    val latitude: Float,
    val longitude: Float,
    val groundSpeed: Float,     // knots
    val trackDegrees: Float,    // true track, degrees
    val positionTimestamp: Long, // epoch millis when lat/lon/gs/track were last updated
    val classification: AircraftClass
) {
    val isEmergencySquawk: Boolean get() = isEmergencySquawkCode(squawk)

    /** Distance in nautical miles from a fixed point to this aircraft. */
    fun distanceNm(queryLat: Double, queryLon: Double): Float {
        val dlat = (latitude - queryLat.toFloat()) * NM_PER_DEG_LAT
        val dlon = (longitude - queryLon.toFloat()) * NM_PER_DEG_LAT *
            cos(queryLat.toFloat() * DEG2RAD)
        return sqrt(dlat * dlat + dlon * dlon)
    }

    /** Bearing in degrees from a fixed point to this aircraft (0=N, 90=E …). */
    fun bearingDeg(queryLat: Double, queryLon: Double): Float {
        val dlon = (longitude - queryLon.toFloat()) * cos(queryLat.toFloat() * DEG2RAD)
        val dlat = latitude - queryLat.toFloat()
        var b = atan2(dlon, dlat) * 180f / PI_F
        if (b < 0) b += 360f
        return b
    }

    /**
     * Seconds until the aircraft exits the query radius, or -1 if unknown.
     * Steps the position forward in 5-second increments along its track until it
     * leaves the circle, capping at 60 minutes to avoid runaway loops.
     */
    fun etaSeconds(queryLatitude: Double, queryLongitude: Double, queryRadius: Float): Int {
        if (groundSpeed < 5.0f || latitude == 0.0f) return -1

        var curLat = latitude
        var curLon = longitude
        val trackRad = trackDegrees * DEG2RAD
        val speedNmPerSec = groundSpeed / 3600.0f

        var sec = 0
        while (sec <= 3600) {
            val dLat = cos(trackRad) * speedNmPerSec * 5.0f / NM_PER_DEG_LAT
            val dLon = sin(trackRad) * speedNmPerSec * 5.0f /
                (NM_PER_DEG_LAT * cos(curLat * DEG2RAD))
            curLat += dLat
            curLon += dLon

            val dlat = (curLat - queryLatitude.toFloat()) * NM_PER_DEG_LAT
            val dlon = (curLon - queryLongitude.toFloat()) * NM_PER_DEG_LAT *
                cos(queryLatitude.toFloat() * DEG2RAD)
            val dist = sqrt(dlat * dlat + dlon * dlon)

            if (dist > queryRadius) return sec
            sec += 5
        }
        return -1 // still inside after 60 min
    }

    /** Adjusts a raw etaSeconds value for elapsed time since positionTimestamp. */
    fun adjustedEta(rawEta: Int): Int {
        if (rawEta < 0) return -1
        val elapsed = ((System.currentTimeMillis() - positionTimestamp) / 1000).toInt()
        return maxOf(0, rawEta - elapsed)
    }

    /** True if the aircraft is moving toward the query point. */
    fun isApproaching(queryLat: Double, queryLon: Double): Boolean {
        if (groundSpeed < 5.0f) return false
        val bearingBack = (bearingDeg(queryLat, queryLon) + 180f) % 360f
        var diff = abs(trackDegrees - bearingBack)
        if (diff > 180f) diff = 360f - diff
        return diff < 90f
    }

    companion object {
        private const val NM_PER_DEG_LAT = 60.0f
        private const val PI_F = Math.PI.toFloat()
        private const val DEG2RAD = PI_F / 180.0f

        fun isEmergencySquawkCode(sq: String): Boolean =
            sq == "7500" || sq == "7600" || sq == "7700"

        /** Meaning of an emergency squawk code (matches the firmware Notifier wording). */
        fun emergencyMeaning(sq: String): String = when (sq) {
            "7500" -> "Hijacking"
            "7600" -> "Radio Failure"
            "7700" -> "General Emergency"
            else -> "Emergency"
        }

        private val COMPASS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

        /** 8-point compass abbreviation for a bearing. */
        fun compassPoint(bearing: Float): String =
            COMPASS[(((bearing + 22.5f) / 45.0f).toInt()) % 8]
    }
}
