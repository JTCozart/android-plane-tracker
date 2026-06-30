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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.ui.openFlightTrack
import com.jtcozart.planetracker.ui.theme.classColor

@Composable
fun HistoryScreen(state: TrackerState, modifier: Modifier = Modifier) {
    if (state.history.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No aircraft detected yet", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.history) { index, ac ->
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { openFlightTrack(context, ac.icao) }
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
                }
            }
        }
    }
}
