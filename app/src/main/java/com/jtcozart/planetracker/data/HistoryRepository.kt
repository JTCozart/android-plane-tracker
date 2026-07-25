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

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore(name = "history")

class HistoryRepository(private val context: Context) {

    private val key = stringPreferencesKey("aircraft_history")

    val history: Flow<List<Aircraft>> = context.historyStore.data.map { prefs ->
        prefs[key]?.let { json -> try { deserialize(json) } catch (_: Exception) { emptyList() } }
            ?: emptyList()
    }

    suspend fun save(history: List<Aircraft>) {
        context.historyStore.edit { it[key] = serialize(history) }
    }

    suspend fun clear() {
        context.historyStore.edit { it.remove(key) }
    }

    companion object {
        fun serialize(history: List<Aircraft>): String {
            val arr = JSONArray()
            history.forEach { ac ->
                arr.put(JSONObject().apply {
                    put("icao", ac.icao)
                    put("callsign", ac.callsign)
                    put("registration", ac.registration)
                    put("type", ac.type)
                    put("owner", ac.owner)
                    put("squawk", ac.squawk)
                    put("altitude", ac.altitude.toDouble())
                    put("latitude", ac.latitude.toDouble())
                    put("longitude", ac.longitude.toDouble())
                    put("groundSpeed", ac.groundSpeed.toDouble())
                    put("trackDegrees", ac.trackDegrees.toDouble())
                    put("positionTimestamp", ac.positionTimestamp)
                    put("classification", ac.classification.name)
                })
            }
            return arr.toString()
        }

        fun deserialize(json: String): List<Aircraft> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Aircraft(
                    icao = o.getString("icao"),
                    callsign = o.optString("callsign", ""),
                    registration = o.optString("registration", ""),
                    type = o.optString("type", ""),
                    owner = o.optString("owner", ""),
                    squawk = o.optString("squawk", ""),
                    altitude = o.optDouble("altitude", 0.0).toFloat(),
                    latitude = o.optDouble("latitude", 0.0).toFloat(),
                    longitude = o.optDouble("longitude", 0.0).toFloat(),
                    groundSpeed = o.optDouble("groundSpeed", 0.0).toFloat(),
                    trackDegrees = o.optDouble("trackDegrees", 0.0).toFloat(),
                    positionTimestamp = o.optLong("positionTimestamp", 0L),
                    classification = try {
                        AircraftClass.valueOf(o.getString("classification"))
                    } catch (_: Exception) {
                        AircraftClass.PRIVATE
                    },
                )
            }
        }
    }
}
