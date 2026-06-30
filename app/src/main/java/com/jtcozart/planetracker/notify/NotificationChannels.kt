package com.jtcozart.planetracker.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.jtcozart.planetracker.model.AircraftClass

/**
 * Notification channels mirroring the firmware's ntfy priorities:
 *   Military = urgent, Medevac = high, Commercial = default, Private = low,
 *   plus a dedicated urgent channel for emergency squawks.
 */
object NotificationChannels {
    const val MILITARY = "ch_military"
    const val MEDEVAC = "ch_medevac"
    const val COMMERCIAL = "ch_commercial"
    const val PRIVATE = "ch_private"
    const val EMERGENCY = "ch_emergency"
    const val SERVICE = "ch_service"

    fun channelFor(cls: AircraftClass): String = when (cls) {
        AircraftClass.MILITARY -> MILITARY
        AircraftClass.MEDEVAC -> MEDEVAC
        AircraftClass.COMMERCIAL -> COMMERCIAL
        AircraftClass.PRIVATE -> PRIVATE
    }

    fun register(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val channels = listOf(
            NotificationChannel(EMERGENCY, "Emergency Squawk", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(MILITARY, "Military Aircraft", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(MEDEVAC, "Medevac Aircraft", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(COMMERCIAL, "Commercial Aircraft", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(PRIVATE, "Private / Other Aircraft", NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(SERVICE, "Tracking Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ongoing notification while PlaneTracker scans in the background"
            },
        )
        nm.createNotificationChannels(channels)
    }
}
