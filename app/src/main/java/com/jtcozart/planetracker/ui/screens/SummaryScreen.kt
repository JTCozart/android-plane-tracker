package com.jtcozart.planetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jtcozart.planetracker.data.TrackerState
import com.jtcozart.planetracker.model.AircraftClass
import com.jtcozart.planetracker.ui.theme.classColor
import com.jtcozart.planetracker.ui.theme.classTextColor

@Composable
fun SummaryScreen(state: TrackerState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Session totals", fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
        AircraftClass.entries.forEach { cls ->
            val count = state.counts[cls] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(classColor(cls))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(cls.displayName, color = classTextColor(cls), fontWeight = FontWeight.Bold)
                Text("$count", color = classTextColor(cls), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
