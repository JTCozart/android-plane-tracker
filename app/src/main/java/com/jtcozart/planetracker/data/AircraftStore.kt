package com.jtcozart.planetracker.data

import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.model.AircraftClass
import org.json.JSONObject

/** Side effects produced by ingesting a scan, consumed by the service to fire notifications. */
data class IngestResult(
    val newDetections: List<Aircraft> = emptyList(),
    val emergencies: List<Aircraft> = emptyList(),
) {
    val hasNewAircraft: Boolean get() = newDetections.isNotEmpty()
}

/**
 * Holds active aircraft, detection history and session class counts.
 * Port of the firmware AircraftStore, minus the NVS persistence (the service owns lifetime).
 */
class AircraftStore {
    private val active = LinkedHashMap<String, Aircraft>()
    private val history = ArrayDeque<Aircraft>()
    private val counts = IntArray(AircraftClass.entries.size)

    var lastAircraftCount = 0
        private set

    val activeAircraft: List<Aircraft> get() = active.values.toList()
    val historyAircraft: List<Aircraft> get() = history.toList()
    fun detectionCount(cls: AircraftClass): Int = counts[cls.ordinal]

    fun clearCounts() = counts.fill(0)

    /**
     * Parse the adsb.lol response, update state, and return the notification-worthy effects.
     * Mirrors AircraftStore::fetch / registerNewAircraft / updateExistingAircraft.
     */
    fun ingest(response: JSONObject, settings: Settings): IngestResult {
        val acArray = response.optJSONArray("ac")
        lastAircraftCount = acArray?.length() ?: 0

        val newDetections = mutableListOf<Aircraft>()
        val emergencies = mutableListOf<Aircraft>()
        val seen = HashSet<String>()

        for (i in 0 until (acArray?.length() ?: 0)) {
            val entry = acArray!!.optJSONObject(i) ?: continue
            val hex = entry.optString("hex", "")
            if (hex.isEmpty()) continue
            val icao = hex.uppercase()

            // POI display filter applies to both new arrivals and ongoing updates.
            if (settings.poiDisplayActive) {
                val type = entry.optString("t", "")
                if (!typeMatchesPoi(type, settings.poiTypes)) continue
            }

            seen.add(icao)

            val existing = active[icao]
            if (existing != null) {
                val updated = updateExisting(existing, entry)
                active[icao] = updated
                // Newly-set emergency squawk fires once, like the firmware.
                if (updated.squawk != existing.squawk && updated.isEmergencySquawk) {
                    emergencies.add(updated)
                }
            } else {
                val aircraft = parseNew(icao, entry)
                active[icao] = aircraft
                counts[aircraft.classification.ordinal]++
                recordInHistory(aircraft)
                newDetections.add(aircraft)
                if (aircraft.isEmergencySquawk) emergencies.add(aircraft)
            }
        }

        pruneStale(seen)
        return IngestResult(newDetections, emergencies)
    }

    private fun parseNew(icao: String, entry: JSONObject): Aircraft {
        val isMilitary = entry.optInt("mil", 0) != 0 || (entry.optInt("dbFlags", 0) and 1) != 0
        val callsign = entry.optString("flight", "").trim()
        val owner = entry.optString("ownOp", "")
        val category = entry.optString("category", "")
        val classification = AircraftClass.classify(callsign, owner, isMilitary, category)
        return Aircraft(
            icao = icao,
            callsign = callsign,
            registration = entry.optString("r", ""),
            type = entry.optString("t", ""),
            owner = owner,
            squawk = entry.optString("squawk", ""),
            altitude = entry.optDouble("alt_baro", 0.0).toFloat(),
            latitude = entry.optDouble("lat", 0.0).toFloat(),
            longitude = entry.optDouble("lon", 0.0).toFloat(),
            groundSpeed = entry.optDouble("gs", 0.0).toFloat(),
            trackDegrees = entry.optDouble("track", 0.0).toFloat(),
            positionTimestamp = System.currentTimeMillis(),
            classification = classification,
        )
    }

    private fun updateExisting(existing: Aircraft, entry: JSONObject): Aircraft =
        existing.copy(
            altitude = entry.optDouble("alt_baro", 0.0).toFloat(),
            latitude = entry.optDouble("lat", 0.0).toFloat(),
            longitude = entry.optDouble("lon", 0.0).toFloat(),
            groundSpeed = entry.optDouble("gs", 0.0).toFloat(),
            trackDegrees = entry.optDouble("track", 0.0).toFloat(),
            squawk = entry.optString("squawk", ""),
            positionTimestamp = System.currentTimeMillis(),
        )

    private fun recordInHistory(aircraft: Aircraft) {
        history.addFirst(aircraft)
        while (history.size > HISTORY_MAX) history.removeLast()
    }

    private fun pruneStale(seen: Set<String>) {
        val iter = active.entries.iterator()
        while (iter.hasNext()) {
            if (!seen.contains(iter.next().key)) iter.remove()
        }
    }

    companion object {
        const val HISTORY_MAX = 5

        /** Whole-token, case-insensitive match of [type] against a CSV [poiList]. */
        fun typeMatchesPoi(type: String, poiList: String): Boolean {
            if (type.isEmpty() || poiList.isBlank()) return false
            return poiList.split(',').any { it.trim().equals(type, ignoreCase = true) }
        }
    }
}
