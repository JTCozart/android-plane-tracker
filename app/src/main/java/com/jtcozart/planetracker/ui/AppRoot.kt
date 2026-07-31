package com.jtcozart.planetracker.ui

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.ui.components.BannerAd
import com.jtcozart.planetracker.ui.components.FlightDetailDialog
import com.jtcozart.planetracker.ui.screens.HistoryScreen
import com.jtcozart.planetracker.ui.screens.LiveScreen
import com.jtcozart.planetracker.ui.screens.MapScreen
import com.jtcozart.planetracker.ui.screens.SettingsScreen
import com.jtcozart.planetracker.ui.theme.PrivateColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private enum class Tab(val label: String, val icon: ImageVector) {
    LIVE("Live", Icons.Filled.Flight),
    MAP("Map", Icons.Filled.Map),
    HISTORY("History", Icons.Filled.History),
    SETTINGS("Settings", Icons.Filled.Settings),
}

private enum class TutorialTarget {
    NONE, NAV_LIVE, NAV_MAP, NAV_HISTORY, NAV_SETTINGS, LOCK_ICON, PLAY_STOP_ICON, NOTIFICATIONS_SECTION
}

private data class TutorialStep(
    val tab: Tab?,
    val target: TutorialTarget,
    val title: String,
    val body: String,
)

private fun navTargetFor(tab: Tab): TutorialTarget = when (tab) {
    Tab.LIVE -> TutorialTarget.NAV_LIVE
    Tab.MAP -> TutorialTarget.NAV_MAP
    Tab.HISTORY -> TutorialTarget.NAV_HISTORY
    Tab.SETTINGS -> TutorialTarget.NAV_SETTINGS
}

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        null, TutorialTarget.NONE, "Welcome to PlaneTracker",
        "PlaneTracker scans for aircraft near you and can alert you when one comes close. " +
            "Here's a quick tour of the app.",
    ),
    TutorialStep(
        Tab.LIVE, TutorialTarget.NAV_LIVE, "Live tab",
        "Aircraft currently detected near you, updated as each scan comes in.",
    ),
    TutorialStep(
        Tab.MAP, TutorialTarget.NAV_MAP, "Map tab",
        "The same detections plotted on a map around your location.",
    ),
    TutorialStep(
        Tab.HISTORY, TutorialTarget.NAV_HISTORY, "History tab",
        "Everything detected today, with running totals by category.",
    ),
    TutorialStep(
        Tab.SETTINGS, TutorialTarget.NAV_SETTINGS, "Settings tab",
        "Tune your scan radius, interval, POI filters, theme, and notifications here.",
    ),
    TutorialStep(
        Tab.LIVE, TutorialTarget.PLAY_STOP_ICON, "Start / stop tracking",
        "Tap this button any time to pause or resume the background scan.",
    ),
    TutorialStep(
        Tab.LIVE, TutorialTarget.LOCK_ICON, "Lock your location",
        "Once a GPS fix is found, tap the lock to freeze the search center, so it won't drift " +
            "as your phone's location updates.",
    ),
    TutorialStep(
        Tab.SETTINGS, TutorialTarget.NOTIFICATIONS_SECTION, "Notifications",
        "Choose which categories page you — military, medevac, commercial, private, POI " +
            "matches, and emergency squawks — or turn notifications off entirely.",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModel: TrackerViewModel,
    requiredPermissions: List<String>,
    pendingIcao: String? = null,
    onPendingIcaoConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tutorialCompleted by viewModel.tutorialCompleted.collectAsStateWithLifecycle()
    val shouldShowReviewPrompt by viewModel.shouldShowReviewPrompt.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val spotted by viewModel.spotted.collectAsStateWithLifecycle()

    fun hasLocation() = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(hasLocation()) }
    var currentTab by remember { mutableStateOf(Tab.LIVE) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showOnlyNotifyMatches by remember { mutableStateOf(false) }
    var selectedAircraftIcao by remember { mutableStateOf<String?>(null) }
    var showFlightNotFound by remember { mutableStateOf(false) }
    var tutorialStepIndex by remember { mutableStateOf(0) }
    val tutorialTargets = remember { mutableStateMapOf<TutorialTarget, Rect>() }
    val settingsScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        if (!viewModel.state.value.running) viewModel.startTracking()
    }

    LaunchedEffect(pendingIcao) {
        if (pendingIcao != null) {
            selectedAircraftIcao = pendingIcao
            onPendingIcaoConsumed()
        }
    }

    val showTutorial = !tutorialCompleted
    if (showTutorial) {
        LaunchedEffect(tutorialStepIndex) {
            TUTORIAL_STEPS[tutorialStepIndex].tab?.let { currentTab = it }
        }
        LaunchedEffect(tutorialStepIndex, currentTab) {
            val step = TUTORIAL_STEPS[tutorialStepIndex]
            if (step.target == TutorialTarget.NOTIFICATIONS_SECTION && currentTab == Tab.SETTINGS) {
                delay(50)
                settingsScrollState.animateScrollTo(settingsScrollState.maxValue)
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear history?") },
            text = { Text("This will permanently delete all detected aircraft history and totals.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); showClearConfirm = false }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (shouldShowReviewPrompt && !showTutorial) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReviewPrompt() },
            title = { Text("Enjoying PlaneTracker?") },
            text = { Text("If you like the app, a quick rating helps a lot.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissReviewPrompt()
                    val activity = context.findActivity()
                    val packageName = context.packageName
                    fun openPlayStoreListing() {
                        val intent = try {
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        } catch (e: Exception) {
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                                )
                            )
                        }
                    }
                    if (activity == null) {
                        openPlayStoreListing()
                    } else {
                        val reviewManager = ReviewManagerFactory.create(context)
                        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
                            if (request.isSuccessful) {
                                reviewManager.launchReviewFlow(activity, request.result)
                            } else {
                                // Google's in-app review flow is unavailable — fall back to the Play Store listing.
                                openPlayStoreListing()
                            }
                        }
                    }
                }) { Text("Rate it") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReviewPrompt() }) { Text("Not now") }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("PlaneTracker")
                            if (state.locationLocked && state.centerLat != null && state.centerLon != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        "%.4f, %.4f".format(state.centerLat, state.centerLon),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (streak.currentStreak > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (streak.securedToday) PrivateColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${streak.currentStreak} day streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (streak.securedToday) PrivateColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { shareApp(context) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share this app")
                        }
                        if (currentTab == Tab.HISTORY && state.history.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirm = true }) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = "Clear history")
                            }
                        }
                        if (state.hasLocation) {
                            IconButton(
                                onClick = { viewModel.toggleLocationLock() },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    tutorialTargets[TutorialTarget.LOCK_ICON] = coords.boundsInRoot()
                                },
                            ) {
                                if (state.locationLocked) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Unlock location")
                                } else {
                                    Icon(Icons.Filled.LockOpen, contentDescription = "Lock location")
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                if (state.running) viewModel.stopTracking() else viewModel.startTracking()
                            },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                tutorialTargets[TutorialTarget.PLAY_STOP_ICON] = coords.boundsInRoot()
                            },
                        ) {
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
                Column {
                    if (currentTab == Tab.LIVE || currentTab == Tab.HISTORY || currentTab == Tab.MAP) {
                        BannerAd()
                    }
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    tutorialTargets[navTargetFor(tab)] = coords.boundsInRoot()
                                },
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val modifier = Modifier.fillMaxSize().padding(padding)
            when (currentTab) {
                Tab.LIVE -> LiveScreen(
                    state, settings, showOnlyNotifyMatches,
                    onShowOnlyNotifyMatchesChange = { showOnlyNotifyMatches = it },
                    onAircraftClick = { ac -> selectedAircraftIcao = ac.icao },
                    modifier = modifier,
                )
                Tab.MAP -> MapScreen(
                    state, settings, showOnlyNotifyMatches,
                    onShowOnlyNotifyMatchesChange = { showOnlyNotifyMatches = it },
                    onAircraftClick = { ac -> selectedAircraftIcao = ac.icao },
                    modifier = modifier,
                )
                Tab.HISTORY -> HistoryScreen(
                    state, settings, showOnlyNotifyMatches,
                    onShowOnlyNotifyMatchesChange = { showOnlyNotifyMatches = it },
                    spotted = spotted,
                    streak = streak,
                    onAircraftClick = { ac -> selectedAircraftIcao = ac.icao },
                    onSpottedClick = { icao -> selectedAircraftIcao = icao },
                    modifier = modifier,
                )
                Tab.SETTINGS -> SettingsScreen(
                    settings, viewModel, modifier, settingsScrollState,
                    onNotificationsSectionPositioned = { rect ->
                        tutorialTargets[TutorialTarget.NOTIFICATIONS_SECTION] = rect
                    },
                )
            }
        }

        val activeAircraft: Aircraft? = selectedAircraftIcao?.let { icao -> state.active.find { it.icao == icao } }
        val selectedAircraft: Aircraft? = activeAircraft
            ?: selectedAircraftIcao?.let { icao -> state.history.find { it.icao == icao } }
        val dialogLat = state.centerLat ?: 0.0
        val dialogLon = state.centerLon ?: 0.0
        if (selectedAircraft != null) {
            FlightDetailDialog(
                aircraft = selectedAircraft,
                centerLat = dialogLat,
                centerLon = dialogLon,
                radiusNm = state.radiusNm,
                inRange = activeAircraft != null,
                alreadySpotted = spotted.any { it.icao == selectedAircraft.icao },
                onSpot = { viewModel.markSpotted(selectedAircraft) },
                onDismiss = { selectedAircraftIcao = null },
            )
        }
        if (showFlightNotFound) {
            AlertDialog(
                onDismissRequest = { showFlightNotFound = false },
                title = { Text("Flight not found") },
                text = { Text("This flight is no longer being tracked and isn't in your history.") },
                confirmButton = {
                    TextButton(onClick = { showFlightNotFound = false }) { Text("OK") }
                },
            )
        }
        // The service loads persisted history asynchronously on cold start, so a notification tap
        // right after launch can race the data becoming available. Wait for it (with a timeout)
        // before concluding the flight genuinely isn't there (e.g. history was cleared).
        LaunchedEffect(selectedAircraftIcao) {
            val icao = selectedAircraftIcao ?: return@LaunchedEffect
            if (viewModel.state.value.active.any { it.icao == icao } ||
                viewModel.state.value.history.any { it.icao == icao }
            ) {
                return@LaunchedEffect
            }
            val found = withTimeoutOrNull(5000) {
                viewModel.state.first { s ->
                    s.active.any { it.icao == icao } || s.history.any { it.icao == icao }
                }
            }
            if (found == null && selectedAircraftIcao == icao) {
                selectedAircraftIcao = null
                showFlightNotFound = true
            }
        }

        if (showTutorial) {
            val step = TUTORIAL_STEPS[tutorialStepIndex]
            TutorialOverlay(
                step = step,
                stepIndex = tutorialStepIndex,
                totalSteps = TUTORIAL_STEPS.size,
                targetRect = tutorialTargets[step.target],
                cardAtTop = tutorialStepIndex >= TUTORIAL_STEPS.size - 3,
                onNext = {
                    if (tutorialStepIndex < TUTORIAL_STEPS.lastIndex) {
                        tutorialStepIndex++
                    } else {
                        viewModel.completeTutorial()
                        coroutineScope.launch { settingsScrollState.scrollTo(0) }
                    }
                },
                onSkip = {
                    viewModel.completeTutorial()
                    coroutineScope.launch { settingsScrollState.scrollTo(0) }
                },
            )
        }
    }
}

@Composable
private fun TutorialOverlay(
    step: TutorialStep,
    stepIndex: Int,
    totalSteps: Int,
    targetRect: Rect?,
    cardAtTop: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    val highlightPadding = with(density) { 8.dp.toPx() }
    val cornerRadiusPx = with(density) { 16.dp.toPx() }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.7f))
            targetRect?.let { r ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(r.left - highlightPadding, r.top - highlightPadding),
                    size = Size(r.width + highlightPadding * 2, r.height + highlightPadding * 2),
                    cornerRadius = CornerRadius(cornerRadiusPx),
                    blendMode = BlendMode.Clear,
                )
            }
        }

        targetRect?.let { r ->
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (r.left - highlightPadding).toInt(),
                            (r.top - highlightPadding).toInt(),
                        )
                    }
                    .size(
                        with(density) { (r.width + highlightPadding * 2).toDp() },
                        with(density) { (r.height + highlightPadding * 2).toDp() },
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            )
        }

        Card(
            modifier = Modifier
                .align(if (cardAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (cardAtTop) 72.dp else 16.dp,
                    bottom = if (cardAtTop) 16.dp else 88.dp,
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${stepIndex + 1} / $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(step.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(step.body, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) { Text("Skip tutorial") }
                    Button(onClick = onNext) {
                        Text(if (stepIndex == totalSteps - 1) "Done" else "Next")
                    }
                }
            }
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
