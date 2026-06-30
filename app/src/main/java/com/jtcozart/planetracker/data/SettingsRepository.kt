package com.jtcozart.planetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** DataStore-backed persistence for [Settings] (replaces the firmware's NVS). */
class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { p -> p.toSettings() }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val next = transform(p.toSettings())
            p[K_RADIUS] = next.radiusNm
            p[K_POLL] = next.pollIntervalSec.coerceAtLeast(Settings.MIN_POLL_INTERVAL_SEC)
            p[K_POI_TYPES] = next.poiTypes
            p[K_POI_ENABLED] = next.poiEnabled
            p[K_NOTIFY] = next.notificationsEnabled
            p[K_NOTIFY_MIL] = next.notifyMilitary
            p[K_NOTIFY_MED] = next.notifyMedevac
            p[K_NOTIFY_COMM] = next.notifyCommercial
            p[K_NOTIFY_PRIV] = next.notifyPrivate
            p[K_NOTIFY_POI] = next.notifyPoi
            p[K_NOTIFY_EMERG] = next.notifyEmergencySquawk
        }
    }

    private fun Preferences.toSettings() = Settings(
        radiusNm = this[K_RADIUS] ?: Settings.DEFAULT_RADIUS_NM,
        pollIntervalSec = this[K_POLL] ?: Settings.DEFAULT_POLL_INTERVAL_SEC,
        poiTypes = this[K_POI_TYPES] ?: "",
        poiEnabled = this[K_POI_ENABLED] ?: false,
        notificationsEnabled = this[K_NOTIFY] ?: true,
        notifyMilitary = this[K_NOTIFY_MIL] ?: true,
        notifyMedevac = this[K_NOTIFY_MED] ?: true,
        notifyCommercial = this[K_NOTIFY_COMM] ?: true,
        notifyPrivate = this[K_NOTIFY_PRIV] ?: true,
        notifyPoi = this[K_NOTIFY_POI] ?: false,
        notifyEmergencySquawk = this[K_NOTIFY_EMERG] ?: true,
    )

    private companion object {
        val K_RADIUS = floatPreferencesKey("radius_nm")
        val K_POLL = intPreferencesKey("poll_interval_sec")
        val K_POI_TYPES = stringPreferencesKey("poi_types")
        val K_POI_ENABLED = booleanPreferencesKey("poi_enabled")
        val K_NOTIFY = booleanPreferencesKey("notifications_enabled")
        val K_NOTIFY_MIL = booleanPreferencesKey("notify_military")
        val K_NOTIFY_MED = booleanPreferencesKey("notify_medevac")
        val K_NOTIFY_COMM = booleanPreferencesKey("notify_commercial")
        val K_NOTIFY_PRIV = booleanPreferencesKey("notify_private")
        val K_NOTIFY_POI = booleanPreferencesKey("notify_poi")
        val K_NOTIFY_EMERG = booleanPreferencesKey("notify_emergency_squawk")
    }
}
