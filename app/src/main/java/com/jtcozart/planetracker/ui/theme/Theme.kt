package com.jtcozart.planetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jtcozart.planetracker.model.AircraftClass

// Class colors match the firmware display palette.
val MilitaryColor = Color(0xFFD32F2F)   // red
val MedevacColor = Color(0xFF1976D2)    // blue
val CommercialColor = Color(0xFF388E3C) // green
val PrivateColor = Color(0xFFF9A825)    // yellow
val RadarGreen = Color(0xFF00E676)

/** Background color for a class card, mirroring the device screen color coding. */
fun classColor(cls: AircraftClass): Color = when (cls) {
    AircraftClass.MILITARY -> MilitaryColor
    AircraftClass.MEDEVAC -> MedevacColor
    AircraftClass.COMMERCIAL -> CommercialColor
    AircraftClass.PRIVATE -> PrivateColor
}

/** Foreground text color for a class card (white on red/blue, black on green/yellow). */
fun classTextColor(cls: AircraftClass): Color = when (cls) {
    AircraftClass.MILITARY, AircraftClass.MEDEVAC -> Color.White
    else -> Color.Black
}

private val DarkColors = darkColorScheme(
    primary = RadarGreen,
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF15151C),
)
private val LightColors = lightColorScheme(primary = Color(0xFF1B5E20))

@Composable
fun PlaneTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
