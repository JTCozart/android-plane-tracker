package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jtcozart.planetracker.data.TrackerState

/**
 * Hosts the two scope views — the device-style Radar and the live OpenStreetMap Map.
 * The selected sub-view is hoisted to the caller; switching is done via the TopAppBar.
 */
@Composable
fun ScopeScreen(
    state: TrackerState,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    when (selected) {
        0 -> RadarScreen(state, modifier.fillMaxSize())
        else -> MapScreen(state, modifier.fillMaxSize())
    }
}
