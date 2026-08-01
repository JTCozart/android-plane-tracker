package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import com.jtcozart.planetracker.data.Settings
import com.jtcozart.planetracker.data.SpottedAircraft
import com.jtcozart.planetracker.data.StreakState
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.model.Aircraft
import com.jtcozart.planetracker.model.AircraftClass
import com.jtcozart.planetracker.ui.components.NotifyFilterToggle
import com.jtcozart.planetracker.ui.shareStreakCard
import com.jtcozart.planetracker.ui.theme.classColor
import com.jtcozart.planetracker.ui.theme.classTextColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryMode { TRACKED, SPOTTED }

@Composable
fun HistoryScreen(
    state: TrackerState,
    settings: Settings,
    showOnlyNotifyMatches: Boolean,
    onShowOnlyNotifyMatchesChange: (Boolean) -> Unit,
    spotted: List<SpottedAircraft> = emptyList(),
    streak: StreakState = StreakState(),
    onAircraftClick: (Aircraft) -> Unit = {},
    onSpottedClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(HistoryMode.TRACKED) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            SegmentedButton(
                selected = mode == HistoryMode.TRACKED,
                onClick = { mode = HistoryMode.TRACKED },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Tracked") }
            SegmentedButton(
                selected = mode == HistoryMode.SPOTTED,
                onClick = { mode = HistoryMode.SPOTTED },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Spotted (${spotted.size})") }
        }

        if (mode == HistoryMode.SPOTTED) {
            SpottedList(spotted, streak, onSpottedClick, Modifier.fillMaxSize())
        } else {
            TrackedHistory(state, settings, showOnlyNotifyMatches, onShowOnlyNotifyMatchesChange, onAircraftClick, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SpottedList(
    spotted: List<SpottedAircraft>,
    streak: StreakState,
    onSpottedClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    if (spotted.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "Nothing spotted yet — tap \"I spotted this!\" on a flight's details to log it here.",
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (streak.currentStreak > 0) {
            item {
                Button(
                    onClick = { shareStreakCard(context, streak, spotted) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                ) {
                    Text("Share my streak")
                }
            }
        }
        itemsIndexed(spotted) { _, s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSpottedClick(s.icao) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape)
                        .background(classColor(s.classification))
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        s.callsign.ifEmpty { s.registration.ifEmpty { s.icao } },
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )
                    Text(
                        "${s.type.ifEmpty { "???" }} • ${s.classification.displayName}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        dateFormat.format(Date(s.spottedTimestamp)),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackedHistory(
    state: TrackerState,
    settings: Settings,
    showOnlyNotifyMatches: Boolean,
    onShowOnlyNotifyMatchesChange: (Boolean) -> Unit,
    onAircraftClick: (Aircraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.history.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No aircraft detected yet", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val history = if (showOnlyNotifyMatches) {
        state.history.filter { settings.notifiesAircraft(it) }
    } else {
        state.history
    }
    val countsByClass = history.groupingBy { it.classification }.eachCount()

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            NotifyFilterToggle(
                showOnlyNotifyMatches = showOnlyNotifyMatches,
                onChange = onShowOnlyNotifyMatchesChange,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }

        // Totals header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Totals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                AircraftClass.entries.forEach { cls ->
                    val count = countsByClass[cls] ?: 0
                    if (count == 0) return@forEach
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(classColor(cls))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(cls.displayName, color = classTextColor(cls), fontWeight = FontWeight.Bold)
                        Text("$count", color = classTextColor(cls), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Detected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
        }

        // History list
        itemsIndexed(history) { _, ac ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAircraftClick(ac) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(16.dp).clip(CircleShape)
                        .background(classColor(ac.classification))
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.icao } },
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )
                    Text(
                        "${ac.type.ifEmpty { "???" }} • ${ac.classification.displayName} • ${ac.altitude.toInt()} ft",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        dateFormat.format(Date(ac.positionTimestamp)),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
