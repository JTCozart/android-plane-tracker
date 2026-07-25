package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.ui.components.AircraftCard
import com.jtcozart.planetracker.ui.components.RadarScope

@Composable
fun LiveScreen(state: TrackerState, modifier: Modifier = Modifier) {
    if (!state.hasLocation) {
        CenterStatus("Acquiring GPS location…", modifier)
        return
    }

    val lat = state.centerLat!!
    val lon = state.centerLon!!
    val config = LocalConfiguration.current
    val radarSize = min(
        config.screenWidthDp.dp - 32.dp,
        config.screenHeightDp.dp * 0.55f,
    )

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            RadarScope(
                modifier = Modifier.size(radarSize),
                aircraft = state.active,
                centerLat = lat,
                centerLon = lon,
                radiusNm = state.radiusNm,
                running = state.running,
            )
            }
        }
        item {
            Text(
                "${state.active.size} overhead • ${state.radiusNm.toInt()} NM range",
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }
        items(state.active, key = { it.icao }) { ac ->
            AircraftCard(ac, lat, lon, state.radiusNm)
        }
    }
}

@Composable
private fun CenterStatus(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
    }
}
