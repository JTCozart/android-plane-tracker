package com.jtcozart.planetracker.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.delay
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.ui.components.NotifyFilterToggle
import com.jtcozart.planetracker.ui.theme.RadarGreen
import com.jtcozart.planetracker.ui.theme.classColor
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlin.math.cos
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/** NM radius within which a callsign label is drawn above the aircraft's marker, to keep the map readable. */
private const val LABEL_RADIUS_NM = 5.0f

/**
 * Live map (OpenStreetMap via osmdroid) centered on the phone, showing the scan-radius
 * circle and each overhead aircraft as a class-colored marker. Aircraft within
 * [LABEL_RADIUS_NM] also get a callsign label above their marker. Tapping a marker or
 * label opens the flight detail popup.
 */
@Composable
fun MapScreen(
    state: TrackerState,
    settings: Settings,
    showOnlyNotifyMatches: Boolean,
    onShowOnlyNotifyMatchesChange: (Boolean) -> Unit,
    onAircraftClick: (Aircraft) -> Unit,
    modifier: Modifier = Modifier,
) {
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

    val visible = if (showOnlyNotifyMatches) {
        state.active.filter { settings.notifiesAircraft(it) }
    } else {
        state.active
    }

    // Ticks 5x/second so aircraft markers glide (dead-reckoned from speed/heading) between
    // polls instead of only jumping when a new fix arrives. Paused with the lifecycle (e.g.
    // app backgrounded) so it doesn't burn CPU/battery while nothing is on screen.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var tickerRunning by remember { mutableStateOf(true) }
    LifecycleStartEffect(Unit) {
        tickerRunning = true
        onStopOrDispose { tickerRunning = false }
    }
    LaunchedEffect(Unit) {
        while (true) {
            if (tickerRunning) nowMillis = System.currentTimeMillis()
            delay(200)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
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

                // Aircraft markers, dead-reckoned to nowMillis so they glide between polls.
                val density = map.resources.displayMetrics.density
                visible.forEach { ac ->
                    val (latI, lonI) = ac.interpolatedPosition(nowMillis)
                    val live = ac.copy(latitude = latI, longitude = lonI)
                    val showLabel = lat != null && lon != null && live.distanceNm(lat, lon) <= LABEL_RADIUS_NM
                    map.overlays.add(aircraftMarker(map, live, density, showLabel) { onAircraftClick(ac) })
                }

                map.invalidate()
            }
        )

        NotifyFilterToggle(
            showOnlyNotifyMatches = showOnlyNotifyMatches,
            onChange = onShowOnlyNotifyMatchesChange,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
        )
    }
}

private fun aircraftMarker(map: MapView, ac: Aircraft, density: Float, showLabel: Boolean, onTap: () -> Unit): Marker =
    Marker(map).apply {
        position = GeoPoint(ac.latitude.toDouble(), ac.longitude.toDouble())
        val color = classColor(ac.classification).toArgb()
        val arrowSizePx = (56 * density).toInt()
        if (showLabel) {
            val label = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.icao } }
            val (bitmap, anchorYFraction) = arrowWithLabel(color, ac.trackDegrees, arrowSizePx, label, density)
            @Suppress("DEPRECATION")
            icon = BitmapDrawable(bitmap)
            setAnchor(Marker.ANCHOR_CENTER, anchorYFraction)
        } else {
            icon = arrow(color, ac.trackDegrees, arrowSizePx)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        title = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.icao } }
        snippet = "${ac.type.ifEmpty { "???" }} • ${ac.altitude.toInt()} ft • tap for details"
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

/** A filled circular marker icon (used for the "You" position). */
private fun dot(color: Int, sizePx: Int): Drawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
    setStroke(3, AndroidColor.BLACK)
    setSize(sizePx, sizePx)
}

/** A triangle arrow pointing in the direction of [trackDegrees] (0 = north). */
private fun arrow(color: Int, trackDegrees: Float, sizePx: Int): Drawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = sizePx / 2f * 0.88f
    canvas.rotate(trackDegrees, cx, cy)
    val path = AndroidPath().apply {
        moveTo(cx, cy - r)                          // nose (pointing up = north)
        lineTo(cx - r * 0.55f, cy + r * 0.7f)      // bottom-left wing
        lineTo(cx + r * 0.55f, cy + r * 0.7f)      // bottom-right wing
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

/**
 * A track arrow with a callsign label drawn above it, on a single bitmap. Returns the
 * bitmap plus the fraction (0..1) down the bitmap where the arrow's center sits, so the
 * marker's anchor can be set to keep the arrow (not the label) pinned to the aircraft's
 * true position.
 */
private fun arrowWithLabel(color: Int, trackDegrees: Float, arrowSizePx: Int, label: String, density: Float): Pair<Bitmap, Float> {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        textSize = 30f * density
        this.isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    val textWidth = textPaint.measureText(label)
    val textHeight = textPaint.descent() - textPaint.ascent()
    val labelPadH = 10 * density
    val labelPadV = 5 * density
    val gap = 4 * density

    val width = maxOf(arrowSizePx, (textWidth + labelPadH * 2).toInt())
    val height = (textHeight + labelPadV * 2 + gap + arrowSizePx).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // Solid pill behind the label so it stays legible over any map tile color.
    val pillRect = android.graphics.RectF(
        width / 2f - textWidth / 2f - labelPadH,
        0f,
        width / 2f + textWidth / 2f + labelPadH,
        textHeight + labelPadV * 2,
    )
    canvas.drawRoundRect(
        pillRect, labelPadV * 2, labelPadV * 2,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.argb(210, 0, 0, 0) },
    )
    canvas.drawText(label, width / 2f, labelPadV - textPaint.ascent(), textPaint)

    val cx = width / 2f
    val cy = textHeight + labelPadV * 2 + gap + arrowSizePx / 2f
    val r = arrowSizePx / 2f * 0.88f
    canvas.save()
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
    canvas.restore()

    return bitmap to (cy / height)
}
