package com.jtcozart.planetracker.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jtcozart.planetracker.ui.screens.HistoryScreen
import com.jtcozart.planetracker.ui.screens.LiveScreen
import com.jtcozart.planetracker.ui.screens.ScopeScreen
import com.jtcozart.planetracker.ui.screens.SettingsScreen
import com.jtcozart.planetracker.ui.screens.SummaryScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    LIVE("Live", Icons.Filled.Flight),
    RADAR("Radar", Icons.Filled.Radar),
    HISTORY("History", Icons.Filled.History),
    SUMMARY("Summary", Icons.Filled.Summarize),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: TrackerViewModel, requiredPermissions: List<String>) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    fun hasLocation() = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(hasLocation()) }
    var currentTab by remember { mutableStateOf(Tab.LIVE) }
    // Sub-tab within the Radar tab (0 = Radar scope, 1 = Map). Hoisted so the bottom
    // Radar button always returns to the radar scope.
    var scopeTab by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) viewModel.startTracking()
    }

    if (!locationGranted) {
        PermissionGate { permissionLauncher.launch(requiredPermissions.toTypedArray()) }
        return
    }

    // Begin scanning on entry, mirroring the firmware's scan-on-boot behavior.
    LaunchedEffect(Unit) {
        if (!viewModel.state.value.running) viewModel.startTracking()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlaneTracker") },
                actions = {
                    if (currentTab == Tab.RADAR) {
                        IconButton(onClick = { scopeTab = 0 }) {
                            Icon(
                                Icons.Filled.Radar,
                                contentDescription = "Radar scope",
                                tint = if (scopeTab == 0)
                                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                                else
                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { scopeTab = 1 }) {
                            Icon(
                                Icons.Filled.Map,
                                contentDescription = "Map view",
                                tint = if (scopeTab == 1)
                                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                                else
                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (state.hasLocation) {
                        IconButton(onClick = { viewModel.toggleLocationLock() }) {
                            if (state.locationLocked) {
                                Icon(Icons.Filled.Lock, contentDescription = "Unlock location")
                            } else {
                                Icon(Icons.Filled.LockOpen, contentDescription = "Lock location")
                            }
                        }
                    }
                    IconButton(onClick = {
                        if (state.running) viewModel.stopTracking() else viewModel.startTracking()
                    }) {
                        if (state.running) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop tracking")
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start tracking")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = {
                            // Tapping Radar (even when already selected) snaps back to the scope.
                            if (tab == Tab.RADAR) scopeTab = 0
                            currentTab = tab
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (currentTab) {
            Tab.LIVE -> LiveScreen(state, modifier)
            Tab.RADAR -> ScopeScreen(state, scopeTab, modifier)
            Tab.HISTORY -> HistoryScreen(state, modifier)
            Tab.SUMMARY -> SummaryScreen(state, modifier)
            Tab.SETTINGS -> SettingsScreen(settings, viewModel, modifier)
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            "PlaneTracker needs your location to scan for aircraft near you, and " +
                "notification permission to alert you when aircraft enter your radius.",
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequest) { Text("Grant permissions") }
    }
}
