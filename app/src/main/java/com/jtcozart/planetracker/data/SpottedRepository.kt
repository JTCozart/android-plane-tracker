package com.jtcozart.planetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.model.AircraftClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.spottedDataStore: DataStore<Preferences> by preferencesDataStore(name = "spotted")

/** A user-confirmed sighting, distinct from the app's automatic detection history. */
data class SpottedAircraft(
    val icao: String,
    val callsign: String,
    val registration: String,
    val type: String,
    val classification: AircraftClass,
    val spottedTimestamp: Long,
)

/** Personal log of aircraft the user has manually marked "I spotted this". */
class SpottedRepository(private val context: Context) {

    private val key = stringPreferencesKey("spotted_aircraft")

    val spotted: Flow<List<SpottedAircraft>> = context.spottedDataStore.data.map { prefs ->
        prefs[key]?.let { json -> try { deserialize(json) } catch (_: Exception) { emptyList() } }
            ?: emptyList()
    }

    suspend fun add(aircraft: Aircraft) {
        context.spottedDataStore.edit { prefs ->
            val current = prefs[key]?.let { try { deserialize(it) } catch (_: Exception) { emptyList() } } ?: emptyList()
            val entry = SpottedAircraft(
                icao = aircraft.icao,
                callsign = aircraft.callsign,
                registration = aircraft.registration,
                type = aircraft.type,
                classification = aircraft.classification,
                spottedTimestamp = System.currentTimeMillis(),
            )
            prefs[key] = serialize(listOf(entry) + current)
        }
    }

    suspend fun clear() {
        context.spottedDataStore.edit { it.remove(key) }
    }

    companion object {
        fun serialize(list: List<SpottedAircraft>): String {
            val arr = JSONArray()
            list.forEach { s ->
                arr.put(JSONObject().apply {
                    put("icao", s.icao)
                    put("callsign", s.callsign)
                    put("registration", s.registration)
                    put("type", s.type)
                    put("classification", s.classification.name)
                    put("spottedTimestamp", s.spottedTimestamp)
                })
            }
            return arr.toString()
        }

        fun deserialize(json: String): List<SpottedAircraft> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SpottedAircraft(
                    icao = o.getString("icao"),
                    callsign = o.optString("callsign", ""),
                    registration = o.optString("registration", ""),
                    type = o.optString("type", ""),
                    classification = try {
                        AircraftClass.valueOf(o.getString("classification"))
                    } catch (_: Exception) {
                        AircraftClass.PRIVATE
                    },
                    spottedTimestamp = o.optLong("spottedTimestamp", 0L),
                )
            }
        }
    }
}
