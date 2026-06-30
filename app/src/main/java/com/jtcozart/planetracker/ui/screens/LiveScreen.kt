package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.ui.components.AircraftCard
import com.jtcozart.planetracker.ui.components.RadarScope

@Composable
fun LiveScreen(state: TrackerState, modifier: Modifier = Modifier) {
    when {
        !state.hasLocation -> CenterStatus("Acquiring GPS location…", modifier)
        !state.hasActiveAircraft -> ScanningView(state, modifier)
        else -> {
            val lat = state.centerLat!!
            val lon = state.centerLon!!
            LazyColumn(
                modifier = modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.active, key = { it.icao }) { ac ->
                    AircraftCard(ac, lat, lon, state.radiusNm)
                }
            }
        }
    }
}

@Composable
private fun ScanningView(state: TrackerState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            RadarScope(modifier = Modifier.size(240.dp))
        }
        val label = if (state.consecutiveFailures > 0) {
            "LOST CONNECTION (${state.consecutiveFailures})"
        } else {
            "SCANNING"
        }
        Text(
            label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            "No aircraft within ${state.radiusNm.toInt()} NM",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun CenterStatus(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
    }
}
