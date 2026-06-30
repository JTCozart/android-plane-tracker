package com.jtcozart.planetracker.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.jtcozart.planetracker.R
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.model.AircraftClass

/**
 * Posts native Android notifications — the local-notification replacement for the
 * firmware's ntfy pushes. Per-class priority and an ADS-B Exchange "Track Flight" action
 * mirror the firmware Notifier.
 */
class Notifier(private val context: Context) {

    private val manager get() = context.getSystemService<NotificationManager>()

    private fun canPost(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notifyDetection(aircraft: Aircraft, settings: Settings) {
        if (!canPost() || !settings.notifiesClass(aircraft.classification)) return

        val cs = aircraft.callsign.ifEmpty { aircraft.type.ifEmpty { "Unknown" } }
        val title = if (settings.poiNotifyActive) {
            "POI: ${aircraft.classification.displayName} Aircraft Detected"
        } else {
            "${aircraft.classification.displayName} Aircraft Detected"
        }
        val body = "$cs (${aircraft.type.ifEmpty { "???" }}) at ${aircraft.altitude.toInt()} ft"

        post(
            channel = NotificationChannels.channelFor(aircraft.classification),
            priority = priorityFor(aircraft.classification),
            id = aircraft.icao.hashCode(),
            title = title,
            body = body,
            icao = aircraft.icao,
        )
    }

    fun notifyEmergencySquawk(aircraft: Aircraft, settings: Settings) {
        if (!canPost() || !settings.notifyEmergencySquawk) return

        val meaning = Aircraft.emergencyMeaning(aircraft.squawk)
        val cs = aircraft.callsign.ifEmpty { aircraft.type.ifEmpty { "Unknown" } }
        post(
            channel = NotificationChannels.EMERGENCY,
            priority = NotificationCompat.PRIORITY_MAX,
            id = ("emerg" + aircraft.icao).hashCode(),
            title = "SQUAWK ${aircraft.squawk} — $meaning",
            body = "$cs squawking ${aircraft.squawk} ($meaning) at ${aircraft.altitude.toInt()} ft",
            icao = aircraft.icao,
            category = NotificationCompat.CATEGORY_ALARM,
        )
    }

    /** Test notification, equivalent to the firmware's "Send Test Notification" button. */
    fun sendTest() {
        if (!canPost()) return
        post(
            channel = NotificationChannels.COMMERCIAL,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            id = "test".hashCode(),
            title = "PlaneTracker Online",
            body = "Notifications are active. You'll be alerted when aircraft enter your scan radius.",
            icao = null,
        )
    }

    private fun priorityFor(cls: AircraftClass): Int = when (cls) {
        AircraftClass.MILITARY -> NotificationCompat.PRIORITY_MAX
        AircraftClass.MEDEVAC -> NotificationCompat.PRIORITY_HIGH
        AircraftClass.COMMERCIAL -> NotificationCompat.PRIORITY_DEFAULT
        AircraftClass.PRIVATE -> NotificationCompat.PRIORITY_LOW
    }

    private fun post(
        channel: String,
        priority: Int,
        id: Int,
        title: String,
        body: String,
        icao: String?,
        category: String? = null,
    ) {
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_plane)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
        category?.let { builder.setCategory(it) }

        if (icao != null) {
            val url = "https://globe.adsbexchange.com/?icao=$icao"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val pending = PendingIntent.getActivity(
                context, icao.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentIntent(pending)
            builder.addAction(R.drawable.ic_stat_plane, "Track Flight", pending)
        }

        manager?.notify(id, builder.build())
    }
}
