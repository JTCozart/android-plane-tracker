package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.ui.components.RadarScope

@Composable
fun RadarScreen(state: TrackerState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        RadarScope(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            aircraft = state.active,
            centerLat = state.centerLat ?: 0.0,
            centerLon = state.centerLon ?: 0.0,
            radiusNm = state.radiusNm,
        )
        Text(
            "${state.active.size} overhead • ${state.radiusNm.toInt()} NM range",
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
