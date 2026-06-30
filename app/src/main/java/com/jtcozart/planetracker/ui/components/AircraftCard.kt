package com.jtcozart.planetracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.ui.openFlightTrack
import com.jtcozart.planetracker.ui.theme.classColor
import com.jtcozart.planetracker.ui.theme.classTextColor

/** A color-coded card for one aircraft, mirroring the device's live detection screen. */
@Composable
fun AircraftCard(
    aircraft: Aircraft,
    centerLat: Double,
    centerLon: Double,
    radiusNm: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val base = classColor(aircraft.classification)
    val text = classTextColor(aircraft.classification)

    // Emergency squawk flashes between red and the class color (like the device + web UI).
    val bg = if (aircraft.isEmergencySquawk) {
        val transition = rememberInfiniteTransition(label = "emergency")
        val t by transition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
            label = "flash",
        )
        lerpColor(Color(0xFFD32F2F), base, t)
    } else {
        animateColorAsState(base, label = "bg").value
    }

    val eta = aircraft.adjustedEta(aircraft.etaSeconds(centerLat, centerLon, radiusNm))
    val distance = aircraft.distanceNm(centerLat, centerLon)
    val bearing = aircraft.bearingDeg(centerLat, centerLon)
    val compass = Aircraft.compassPoint(bearing)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { openFlightTrack(context, aircraft.icao) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val label = aircraft.callsign.ifEmpty { aircraft.registration.ifEmpty { aircraft.icao } }
        // Underlined to signal it opens the live flight path on ADS-B Exchange.
        Text(
            label,
            color = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            "${aircraft.type.ifEmpty { "???" }}  •  ${aircraft.classification.displayName}",
            color = text, fontSize = 14.sp,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("ALT", "${aircraft.altitude.toInt()} ft", text)
            Stat("DIST", String.format("%.1f NM %s", distance, compass), text)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("ETA OUT", if (eta >= 0) formatEta(eta) else "—", text)
            Stat("SQUAWK", aircraft.squawk.ifEmpty { "----" }, text)
        }
        if (aircraft.isEmergencySquawk) {
            Text(
                "⚠ ${aircraft.squawk} — ${Aircraft.emergencyMeaning(aircraft.squawk)}",
                color = text, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

private fun formatEta(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
