package com.jtcozart.planetracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.ui.theme.RadarGreen
import com.jtcozart.planetracker.ui.theme.classColor
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Top-down radar scope. Renders the sweep (always) and aircraft as triangles pointing
 * in their direction of travel, positioned by bearing/distance — port of the device radar view.
 */
@Composable
fun RadarScope(
    modifier: Modifier = Modifier,
    aircraft: List<Aircraft> = emptyList(),
    centerLat: Double = 0.0,
    centerLon: Double = 0.0,
    radiusNm: Float = 5f,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep",
    )

    Canvas(modifier = modifier) {
        val r = min(size.width, size.height) / 2f * 0.9f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Range rings.
        for (i in 1..3) {
            drawCircle(
                color = RadarGreen.copy(alpha = 0.25f),
                radius = r * i / 3f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
        }
        // Cross-hairs.
        drawLine(RadarGreen.copy(alpha = 0.2f), Offset(center.x - r, center.y), Offset(center.x + r, center.y), 2f)
        drawLine(RadarGreen.copy(alpha = 0.2f), Offset(center.x, center.y - r), Offset(center.x, center.y + r), 2f)

        // Sweep line.
        rotate(sweep, pivot = center) {
            drawLine(RadarGreen, center, Offset(center.x, center.y - r), strokeWidth = 3f)
        }

        // Aircraft triangles.
        aircraft.forEach { ac ->
            val dist = ac.distanceNm(centerLat, centerLon)
            if (dist > radiusNm) return@forEach
            val bearing = ac.bearingDeg(centerLat, centerLon)
            val rad = Math.toRadians(bearing.toDouble())
            val px = center.x + (sin(rad) * (dist / radiusNm) * r).toFloat()
            val py = center.y - (cos(rad) * (dist / radiusNm) * r).toFloat()
            drawTriangle(Offset(px, py), ac.trackDegrees, classColor(ac.classification))
        }
    }
}

private fun DrawScope.drawTriangle(at: Offset, headingDeg: Float, color: Color) {
    val s = 14f
    rotate(headingDeg, pivot = at) {
        val path = Path().apply {
            moveTo(at.x, at.y - s)        // nose
            lineTo(at.x - s * 0.7f, at.y + s * 0.7f)
            lineTo(at.x + s * 0.7f, at.y + s * 0.7f)
            close()
        }
        drawPath(path, color)
    }
}
