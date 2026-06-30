package com.jtcozart.planetracker.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.ui.openFlightTrack
import com.jtcozart.planetracker.ui.theme.RadarGreen
import com.jtcozart.planetracker.ui.theme.classColor
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlin.math.cos
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Live map (OpenStreetMap via osmdroid) centered on the phone, showing the scan-radius
 * circle and each overhead aircraft as a class-colored marker. Tapping a marker opens
 * its flight path on ADS-B Exchange.
 */
@Composable
fun MapScreen(state: TrackerState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(11.0)
        }
    }
    // Re-fit only on the first fix and whenever the radius changes, so GPS jitter
    // and user panning don't yank the view.
    var lastFitRadius by remember { mutableStateOf<Float?>(null) }

    // Pause/resume tile downloads with the lifecycle.
    LifecycleStartEffect(mapView) {
        mapView.onResume()
        onStopOrDispose { mapView.onPause() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            val lat = state.centerLat
            val lon = state.centerLon
            if (lat != null && lon != null && lastFitRadius != state.radiusNm) {
                val bbox = radiusBoundingBox(lat, lon, state.radiusNm)
                // post() defers until the MapView has been laid out and has a size.
                map.post { map.zoomToBoundingBox(bbox, false, 24) }
                lastFitRadius = state.radiusNm
            }

            map.overlays.clear()

            if (lat != null && lon != null) {
                // Scan-radius circle.
                val circle = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(GeoPoint(lat, lon), state.radiusNm * 1852.0)
                    fillPaint.color = AndroidColor.argb(40, 0, 230, 118)
                    outlinePaint.color = RadarGreen.toArgb()
                    outlinePaint.strokeWidth = 3f
                }
                map.overlays.add(circle)

                // Your position.
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = dot(AndroidColor.rgb(33, 150, 243), 28)
                        title = "You"
                    }
                )
            }

            // Aircraft markers.
            state.active.forEach { ac ->
                map.overlays.add(aircraftMarker(map, ac) { openFlightTrack(context, ac.icao) })
            }

            map.invalidate()
        }
    )
}

private fun aircraftMarker(map: MapView, ac: Aircraft, onTap: () -> Unit): Marker =
    Marker(map).apply {
        position = GeoPoint(ac.latitude.toDouble(), ac.longitude.toDouble())
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = dot(classColor(ac.classification).toArgb(), 36)
        title = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.icao } }
        snippet = "${ac.type.ifEmpty { "???" }} • ${ac.altitude.toInt()} ft • tap to track"
        setOnMarkerClickListener { _, _ ->
            onTap()
            true
        }
    }

/** A square bounding box around the scan radius, so the circle fills the map viewport. */
private fun radiusBoundingBox(lat: Double, lon: Double, radiusNm: Float): BoundingBox {
    val meters = radiusNm * 1852.0
    val latDelta = meters / 111_320.0
    val lonDelta = meters / (111_320.0 * cos(Math.toRadians(lat)))
    return BoundingBox(lat + latDelta, lon + lonDelta, lat - latDelta, lon - lonDelta)
}

/** A filled circular marker icon in the given ARGB color. */
private fun dot(color: Int, sizePx: Int): Drawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
    setStroke(3, AndroidColor.BLACK)
    setSize(sizePx, sizePx)
}
