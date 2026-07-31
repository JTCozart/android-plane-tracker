package com.jtcozart.planetracker.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.ui.openFlightTrack
import com.jtcozart.planetracker.ui.theme.classColor
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Full detail popup for a single aircraft: every known field, plus a small map showing
 * its current position and a straight-line projected flight path.
 */
@Composable
fun FlightDetailDialog(
    aircraft: Aircraft,
    centerLat: Double,
    centerLon: Double,
    radiusNm: Float,
    inRange: Boolean = true,
    alreadySpotted: Boolean = false,
    onSpot: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val label = aircraft.callsign.ifEmpty { aircraft.registration.ifEmpty { aircraft.icao } }
    val distance = aircraft.distanceNm(centerLat, centerLon)
    val bearing = aircraft.bearingDeg(centerLat, centerLon)
    val compass = Aircraft.compassPoint(bearing)
    val eta = aircraft.adjustedEta(aircraft.etaSeconds(centerLat, centerLon, radiusNm))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${aircraft.type.ifEmpty { "???" }}  •  ${aircraft.classification.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                if (!inRange) {
                    Surface(
                        color = Color(0xFFFFF3CD),
                        contentColor = Color(0xFF664D03),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text(
                            "This aircraft is no longer in range, showing its last known position.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailStat("ALTITUDE", "${aircraft.altitude.toInt()} ft")
                    DetailStat("SPEED", "${aircraft.groundSpeed.toInt()} kt")
                    DetailStat("TRACK", "${aircraft.trackDegrees.toInt()}°")
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailStat("DISTANCE", String.format("%.1f NM %s", distance, compass))
                    DetailStat("ETA OUT", if (eta >= 0) "%d:%02d".format(eta / 60, eta % 60) else "—")
                    DetailStat("SQUAWK", aircraft.squawk.ifEmpty { "----" })
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailStat("REGISTRATION", aircraft.registration.ifEmpty { "—" })
                    DetailStat("ICAO", aircraft.icao)
                }
                if (aircraft.owner.isNotBlank()) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text("OWNER/OPERATOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(aircraft.owner, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (aircraft.isEmergencySquawk) {
                    Text(
                        "⚠ ${aircraft.squawk} — ${Aircraft.emergencyMeaning(aircraft.squawk)}",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Text(
                    "Projected flight path",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                ) {
                    ProjectedPathMap(aircraft)
                }

                if (onSpot != null && inRange) {
                    if (alreadySpotted) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            Icon(Icons.Filled.RemoveRedEye, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Spotted", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onSpot,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            Icon(Icons.Filled.RemoveRedEye, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  I spotted this!", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = { openFlightTrack(context, aircraft.icao) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text("Track on ADS-B Exchange")
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProjectedPathMap(aircraft: Aircraft) {
    val markerColor = classColor(aircraft.classification).toArgb()
    val track = remember(aircraft.icao, aircraft.latitude, aircraft.longitude, aircraft.trackDegrees, aircraft.groundSpeed) {
        aircraft.projectedTrack()
    }
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
            }
        },
        update = { map ->
            map.overlays.clear()

            val points = track.map { (lat, lon) -> GeoPoint(lat.toDouble(), lon.toDouble()) }

            if (points.size > 1) {
                map.overlays.add(
                    Polyline(map).apply {
                        setPoints(points)
                        outlinePaint.color = markerColor
                        outlinePaint.strokeWidth = 6f
                    }
                )
            }

            val density = map.resources.displayMetrics.density
            map.overlays.add(
                Marker(map).apply {
                    position = points.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = planeArrow(markerColor, aircraft.trackDegrees, (56 * density).toInt())
                    title = aircraft.callsign.ifEmpty { aircraft.icao }
                }
            )

            val bbox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
            map.post { map.zoomToBoundingBox(bbox, false, 48) }

            map.invalidate()
        },
    )
}

/** A triangle arrow pointing in the direction of [trackDegrees] (0 = north), matching the Map tab's markers. */
private fun planeArrow(color: Int, trackDegrees: Float, sizePx: Int): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = sizePx / 2f * 0.88f
    canvas.rotate(trackDegrees, cx, cy)
    val path = AndroidPath().apply {
        moveTo(cx, cy - r)
        lineTo(cx - r * 0.55f, cy + r * 0.7f)
        lineTo(cx + r * 0.55f, cy + r * 0.7f)
        close()
    }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    })
    @Suppress("DEPRECATION")
    return BitmapDrawable(bitmap)
}
