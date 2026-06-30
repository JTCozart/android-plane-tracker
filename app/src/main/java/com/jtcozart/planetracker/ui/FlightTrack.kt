package com.jtcozart.planetracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the live flight path for an aircraft on ADS-B Exchange by ICAO hex —
 * the same destination as the firmware's "Track Flight" notification action.
 */
fun openFlightTrack(context: Context, icao: String) {
    if (icao.isBlank()) return
    val url = "https://globe.adsbexchange.com/?icao=$icao"
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
